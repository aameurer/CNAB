package com.compara.retorno.controller;

import com.compara.retorno.model.Transacao;
import com.compara.retorno.model.TipoOrigem;
import com.compara.retorno.repository.TransacaoRepository;
import com.compara.retorno.service.CnabParserService;
import com.compara.retorno.service.TransactionService;
import com.compara.retorno.util.DateUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private com.compara.retorno.service.PdfService pdfService;

    @GetMapping("/")
    public String dashboard(Model model, 
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) String filter,
                          @RequestParam(required = false) java.time.LocalDate startDate,
                          @RequestParam(required = false) java.time.LocalDate endDate,
                          @RequestParam(required = false) Boolean useCreditDate) {
        
        // Determine effective useCreditDate before standardizing dates
        boolean effectiveUseCreditDate = (useCreditDate != null) ? useCreditDate : (startDate == null);

        // Standardize dates
        java.time.LocalDate[] dates = DateUtils.validateAndFixRange(startDate, endDate);
        startDate = dates[0];
        endDate = dates[1];
        
        // Load Stats
        TransactionService.DashboardStats stats = transactionService.getStats(startDate, endDate, effectiveUseCreditDate);
        model.addAttribute("stats", stats);
        
        Sort sort = Sort.by(
                Sort.Order.asc("nomePagador").ignoreCase(),
                Sort.Order.asc("nossoNumero"),
                Sort.Order.asc("tipoOrigem"),
                Sort.Order.desc("statusConciliacao"),
                Sort.Order.desc(effectiveUseCreditDate ? "dataCredito" : "dataOcorrencia")
        );
        PageRequest pageable = PageRequest.of(page, 10, sort);
        Page<Transacao> transactions;
        
        if (filter != null && !filter.isEmpty()) {
            if (filter.equals("DIVERGENTE")) {
                transactions = effectiveUseCreditDate ? 
                    repository.findByStatusConciliacaoAndDataCreditoBetween("DIVERGENTE", startDate, endDate, pageable) :
                    repository.findByStatusConciliacaoAndDataOcorrenciaBetween("DIVERGENTE", startDate, endDate, pageable);
            } else if (filter.equals("API")) {
                transactions = effectiveUseCreditDate ?
                    repository.findByTipoOrigemAndDataCreditoBetween(TipoOrigem.API, startDate, endDate, pageable) :
                    repository.findByTipoOrigemAndDataOcorrenciaBetween(TipoOrigem.API, startDate, endDate, pageable);
            } else {
                transactions = effectiveUseCreditDate ?
                    repository.findByDataCreditoBetween(startDate, endDate, pageable) :
                    repository.findByDataOcorrenciaBetween(startDate, endDate, pageable);
            }
        } else {
            transactions = effectiveUseCreditDate ?
                repository.findByDataCreditoBetween(startDate, endDate, pageable) :
                repository.findByDataOcorrenciaBetween(startDate, endDate, pageable);
        }
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("filter", filter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("useCreditDate", effectiveUseCreditDate);
        
        return "dashboard";
    }

    @GetMapping("/import")
    public String importPage(Model model) {
        return "importacao";
    }

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("apiFiles") MultipartFile[] apiFiles,
                            @RequestParam("geralFiles") MultipartFile[] geralFiles,
                            RedirectAttributes redirectAttributes) {
        
        try {
            java.util.List<String> skippedFiles = new java.util.ArrayList<>();
            int processedCount = 0;

            // Process API Files
            for (MultipartFile file : apiFiles) {
                if (!file.isEmpty()) {
                    if (transactionService.isFileAlreadyImported(file.getOriginalFilename())) {
                        skippedFiles.add(file.getOriginalFilename());
                        continue;
                    }
                    List<Transacao> trs = parserService.parseFile(file, "API");
                    transactionService.saveAll(trs);
                    processedCount++;
                }
            }
            
            // Process Geral Files
            for (MultipartFile file : geralFiles) {
                if (!file.isEmpty()) {
                    if (transactionService.isFileAlreadyImported(file.getOriginalFilename())) {
                        skippedFiles.add(file.getOriginalFilename());
                        continue;
                    }
                    List<Transacao> trs = parserService.parseFile(file, "GERAL");
                    transactionService.saveAll(trs);
                    processedCount++;
                }
            }
            
            if (processedCount > 0) {
                // Trigger Reconciliation
                transactionService.performReconciliation();
            }
            
            if (!skippedFiles.isEmpty()) {
                String skippedMsg = "Arquivos já existentes ignorados: " + String.join(", ", skippedFiles);
                if (processedCount > 0) {
                    redirectAttributes.addFlashAttribute("message", "Importação parcial concluída. " + skippedMsg);
                    redirectAttributes.addFlashAttribute("messageType", "warning");
                } else {
                    redirectAttributes.addFlashAttribute("message", "Importação cancelada. " + skippedMsg);
                    redirectAttributes.addFlashAttribute("messageType", "warning");
                }
            } else if (processedCount > 0) {
                redirectAttributes.addFlashAttribute("message", "Arquivos processados com sucesso!");
                redirectAttributes.addFlashAttribute("messageType", "success");
            } else {
                 redirectAttributes.addFlashAttribute("message", "Nenhum arquivo selecionado.");
                 redirectAttributes.addFlashAttribute("messageType", "info");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao processar arquivos: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        
        return "redirect:/import";
    }
    
    @GetMapping("/files")
    public String gerenciarArquivos(Model model) {
        List<String> files = transactionService.getAllFileSources();
        model.addAttribute("files", files);
        return "gerenciar_arquivos";
    }

    @PostMapping("/files/delete")
    public String deleteFiles(@RequestParam(required = false) List<String> selectedFiles, RedirectAttributes redirectAttributes) {
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            try {
                transactionService.deleteByFileSources(selectedFiles);
                redirectAttributes.addFlashAttribute("message", "Arquivos selecionados foram excluídos com sucesso!");
                redirectAttributes.addFlashAttribute("messageType", "success");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("message", "Erro ao excluir arquivos: " + e.getMessage());
                redirectAttributes.addFlashAttribute("messageType", "danger");
            }
        } else {
            redirectAttributes.addFlashAttribute("message", "Nenhum arquivo selecionado para exclusão.");
            redirectAttributes.addFlashAttribute("messageType", "warning");
        }
        return "redirect:/files";
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
                             @RequestParam(required = false) Boolean useCreditDate,
                             @RequestParam(defaultValue = "false") boolean onlyDivergences) {
        
        boolean effectiveUseCreditDate = (useCreditDate != null) ? useCreditDate : (startDate == null);
        
        java.time.LocalDate[] dates = DateUtils.validateAndFixRange(startDate, endDate);
        startDate = dates[0];
        endDate = dates[1];
        
        List<TransactionService.ComparisonResult> results = transactionService.compareTransactions(startDate, endDate, effectiveUseCreditDate, onlyDivergences);
        
        model.addAttribute("results", results);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("useCreditDate", effectiveUseCreditDate);
        model.addAttribute("onlyDivergences", onlyDivergences);
        
        return "analise_datas";
    }

    @GetMapping("/analise-datas/pdf")
    public void exportPdf(jakarta.servlet.http.HttpServletResponse response,
                          @RequestParam(required = false) java.time.LocalDate startDate,
                          @RequestParam(required = false) java.time.LocalDate endDate,
                          @RequestParam(required = false) Boolean useCreditDate,
                          @RequestParam(defaultValue = "false") boolean onlyDivergences) throws java.io.IOException {
        
        boolean effectiveUseCreditDate = (useCreditDate != null) ? useCreditDate : (startDate == null);
        java.time.LocalDate[] dates = DateUtils.validateAndFixRange(startDate, endDate);
        startDate = dates[0];
        endDate = dates[1];
        
        List<TransactionService.ComparisonResult> results = transactionService.compareTransactions(startDate, endDate, effectiveUseCreditDate, onlyDivergences);
        
        try {
            byte[] content = pdfService.generateAnaliseReport(results, startDate, endDate);
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"analise_api_geral_" + startDate + "_" + endDate + ".pdf\"");
            response.setHeader("Content-Length", String.valueOf(content.length));
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            try (java.io.OutputStream os = response.getOutputStream()) {
                os.write(content);
                os.flush();
            }
                
        } catch (com.lowagie.text.DocumentException e) {
            throw new java.io.IOException("Error generating PDF", e);
        }
    }

    @GetMapping("/transacao/{id}")
    public String detalhesTransacao(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        Transacao transacao = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Transação inválida Id: " + id));
        model.addAttribute("transacao", transacao);
        return "detalhes_transacao";
    }

    @GetMapping("/analise-datas/export")
    public void exportCsv(jakarta.servlet.http.HttpServletResponse response,
                          @RequestParam(required = false) java.time.LocalDate startDate,
                          @RequestParam(required = false) java.time.LocalDate endDate) throws java.io.IOException {
        
        if (startDate == null) startDate = java.time.LocalDate.now().minusDays(1);
        if (endDate == null) endDate = java.time.LocalDate.now().minusDays(1);

        if (startDate.isAfter(endDate)) {
            java.time.LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"analise_datas.csv\"");
        
        List<Transacao> list = transactionService.findAllByPeriod(startDate, endDate);
        
        java.io.PrintWriter writer = response.getWriter();
        writer.println("Origem;Nosso Numero;Pagador;Data Ocorrencia;Data Credito;Diferenca (Dias);Valor Pago");
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Transacao t : list) {
            long diff = 0;
            if (t.getDataOcorrencia() != null && t.getDataCredito() != null) {
                diff = java.time.temporal.ChronoUnit.DAYS.between(t.getDataOcorrencia(), t.getDataCredito());
            }
            
            writer.printf("%s;%s;%s;%s;%s;%d;%s%n",
                t.getTipoOrigem(),
                t.getNossoNumero(),
                t.getNomePagador(),
                t.getDataOcorrencia() != null ? t.getDataOcorrencia().format(formatter) : "",
                t.getDataCredito() != null ? t.getDataCredito().format(formatter) : "",
                diff,
                t.getValorPago()
            );
        }
    }
}
