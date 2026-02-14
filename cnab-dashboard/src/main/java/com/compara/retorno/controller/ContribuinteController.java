package com.compara.retorno.controller;

import com.compara.retorno.model.Transacao;
import com.compara.retorno.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class ContribuinteController {

    @Autowired
    private TransactionService service;

    @Autowired
    private com.compara.retorno.service.PdfService pdfService;

    @GetMapping("/contribuintes")
    public String searchPage(Model model,
            HttpServletRequest request,
            @RequestParam(required = false) String nomePagador,
            @RequestParam(required = false) BigDecimal valorPago,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @org.springframework.web.bind.annotation.RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {

        long start = System.currentTimeMillis();
        Page<Transacao> results;
        long took;
        try {
            PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "data_ocorrencia"));
            results = service.searchTransactions(nomePagador, valorPago, pageable);
            took = System.currentTimeMillis() - start;
        } catch (Exception ex) {
            results = org.springframework.data.domain.Page.empty();
            took = System.currentTimeMillis() - start;
            model.addAttribute("error", "Falha ao consultar transações: " + ex.getMessage());
        }

        model.addAttribute("activePage", "contribuintes");
        model.addAttribute("results", results);
        model.addAttribute("nomePagador", nomePagador);
        model.addAttribute("valorPago", valorPago);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("took", took);

        return hxRequest ? "contribuintes :: content" : "contribuintes";
    }

    @GetMapping("/contribuintes/pdf")
    public org.springframework.http.ResponseEntity<byte[]> downloadPdf(
            @RequestParam(required = false) String nomePagador,
            @RequestParam(required = false) BigDecimal valorPago) {

        try {
            java.util.List<Transacao> transactions = service.searchTransactionsList(nomePagador, valorPago);
            byte[] pdfContent = pdfService.generateTransactionsReport(transactions);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            String filename = "relatorio_transacoes.pdf";
            headers.setContentDispositionFormData(filename, filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new org.springframework.http.ResponseEntity<>(pdfContent, headers,
                    org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    // PDF and Excel export endpoints removed/commented out as they were specific to
    // Contribuinte model
    // and would need to be reimplemented for Transacao if needed.
}
