package com.compara.retorno.controller;

import com.compara.retorno.model.Transacao;
import com.compara.retorno.repository.TransacaoRepository;
import com.compara.retorno.service.CnabParserService;
import com.compara.retorno.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private CnabParserService parserService;

    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private TransacaoRepository repository;

    @GetMapping("/")
    public String dashboard(Model model, 
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) String filter,
                          @RequestParam(required = false) java.time.LocalDate startDate,
                          @RequestParam(required = false) java.time.LocalDate endDate,
                          @RequestParam(defaultValue = "false") boolean useCreditDate) {
        
        // Default to yesterday if not set
        if (startDate == null) startDate = java.time.LocalDate.now().minusDays(1);
        if (endDate == null) endDate = java.time.LocalDate.now().minusDays(1);
        
        // Load Stats
        TransactionService.DashboardStats stats = transactionService.getStats(startDate, endDate, useCreditDate);
        model.addAttribute("stats", stats);
        
        // Load Table Data
        PageRequest pageable = PageRequest.of(page, 10, Sort.by(useCreditDate ? "dataCredito" : "dataOcorrencia").descending());
        Page<Transacao> transactions;
        
        if (filter != null && !filter.isEmpty()) {
            if (filter.equals("DIVERGENTE")) {
                transactions = useCreditDate ? 
                    repository.findByStatusConciliacaoAndDataCreditoBetween("DIVERGENTE", startDate, endDate, pageable) :
                    repository.findByStatusConciliacaoAndDataOcorrenciaBetween("DIVERGENTE", startDate, endDate, pageable);
            } else if (filter.equals("API")) {
                transactions = useCreditDate ?
                    repository.findByTipoOrigemAndDataCreditoBetween("API", startDate, endDate, pageable) :
                    repository.findByTipoOrigemAndDataOcorrenciaBetween("API", startDate, endDate, pageable);
            } else {
                transactions = useCreditDate ?
                    repository.findByDataCreditoBetween(startDate, endDate, pageable) :
                    repository.findByDataOcorrenciaBetween(startDate, endDate, pageable);
            }
        } else {
            transactions = useCreditDate ?
                repository.findByDataCreditoBetween(startDate, endDate, pageable) :
                repository.findByDataOcorrenciaBetween(startDate, endDate, pageable);
        }
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("filter", filter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("useCreditDate", useCreditDate);
        
        return "dashboard";
    }

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("apiFiles") MultipartFile[] apiFiles,
                            @RequestParam("geralFiles") MultipartFile[] geralFiles,
                            RedirectAttributes redirectAttributes) {
        
        try {
            // Process API Files
            for (MultipartFile file : apiFiles) {
                if (!file.isEmpty()) {
                    List<Transacao> trs = parserService.parseFile(file, "API");
                    transactionService.saveAll(trs);
                }
            }
            
            // Process Geral Files
            for (MultipartFile file : geralFiles) {
                if (!file.isEmpty()) {
                    List<Transacao> trs = parserService.parseFile(file, "GERAL");
                    transactionService.saveAll(trs);
                }
            }
            
            // Trigger Reconciliation
            transactionService.performReconciliation();
            
            redirectAttributes.addFlashAttribute("message", "Arquivos processados com sucesso!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao processar arquivos: " + e.getMessage());
        }
        
        return "redirect:/";
    }
    
    @PostMapping("/clear")
    public String clearDatabase(RedirectAttributes redirectAttributes) {
        transactionService.clearAll();
        redirectAttributes.addFlashAttribute("message", "Banco de dados limpo!");
        return "redirect:/";
    }

    @GetMapping("/analise-datas")
    public String analiseDatas(Model model,
                             @RequestParam(required = false) java.time.LocalDate startDate,
                             @RequestParam(required = false) java.time.LocalDate endDate,
                             @RequestParam(defaultValue = "false") boolean useCreditDate,
                             @RequestParam(defaultValue = "false") boolean onlyDivergences) {
        
        // Default to yesterday if not set
        if (startDate == null) startDate = java.time.LocalDate.now().minusDays(1);
        if (endDate == null) endDate = java.time.LocalDate.now().minusDays(1);
        
        // Use the new comparison logic instead of simple pagination
        List<TransactionService.ComparisonResult> results = transactionService.compareTransactions(startDate, endDate, useCreditDate);
        
        if (onlyDivergences) {
            results = results.stream()
                .filter(TransactionService.ComparisonResult::isDivergent)
                .collect(java.util.stream.Collectors.toList());
        }
        
        model.addAttribute("results", results);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("useCreditDate", useCreditDate);
        model.addAttribute("onlyDivergences", onlyDivergences);
        
        return "analise_datas";
    }

    @GetMapping("/analise-datas/export")
    public void exportCsv(jakarta.servlet.http.HttpServletResponse response,
                          @RequestParam(required = false) java.time.LocalDate startDate,
                          @RequestParam(required = false) java.time.LocalDate endDate) throws java.io.IOException {
        
        if (startDate == null) startDate = java.time.LocalDate.now().minusDays(1);
        if (endDate == null) endDate = java.time.LocalDate.now().minusDays(1);
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"analise_datas.csv\"");
        
        List<Transacao> list = transactionService.findAllByPeriod(startDate, endDate);
        
        java.io.PrintWriter writer = response.getWriter();
        writer.println("Origem;Nosso Numero;Pagador;Data Ocorrencia;Data Credito;Diferenca (Dias);Valor Pago");
        
        for (Transacao t : list) {
            long diff = 0;
            if (t.getDataOcorrencia() != null && t.getDataCredito() != null) {
                diff = java.time.temporal.ChronoUnit.DAYS.between(t.getDataOcorrencia(), t.getDataCredito());
            }
            
            writer.printf("%s;%s;%s;%s;%s;%d;%s%n",
                t.getTipoOrigem(),
                t.getNossoNumero(),
                t.getNomePagador(),
                t.getDataOcorrencia(),
                t.getDataCredito() != null ? t.getDataCredito() : "",
                diff,
                t.getValorPago()
            );
        }
    }
}
