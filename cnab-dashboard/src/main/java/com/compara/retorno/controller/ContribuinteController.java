package com.compara.retorno.controller;

import com.compara.retorno.model.Contribuinte;
import com.compara.retorno.service.ContribuinteService;
import com.compara.retorno.service.ContribuinteReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ContribuinteController {

    @Autowired
    private ContribuinteService service;

    @Autowired
    private ContribuinteReportService reportService;

    @GetMapping("/contribuintes")
    public String searchPage(Model model,
                             HttpServletRequest request,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String bairro,
                             @RequestParam(required = false) String atividade,
                             @RequestParam(required = false) String situacao,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {

        long start = System.currentTimeMillis();
        Page<Contribuinte> results;
        long took;
        try {
            results = service.search(q, bairro, atividade, situacao, page, size,
                    null,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
            took = System.currentTimeMillis() - start;
        } catch (Exception ex) {
            results = org.springframework.data.domain.Page.empty();
            took = System.currentTimeMillis() - start;
            model.addAttribute("error", "Falha ao consultar contribuintes. Verifique a conexão com o banco de dados ou tente novamente.");
        }

        model.addAttribute("activePage", "contribuintes");
        model.addAttribute("results", results);
        model.addAttribute("q", q);
        model.addAttribute("bairro", bairro);
        model.addAttribute("atividade", atividade);
        model.addAttribute("situacao", situacao);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("took", took);
        return "contribuintes";
    }

    @GetMapping("/contribuintes/pdf")
    public void exportPdf(jakarta.servlet.http.HttpServletResponse response,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String bairro,
                          @RequestParam(required = false) String atividade,
                          @RequestParam(required = false) String situacao) throws java.io.IOException {
        Page<Contribuinte> page = service.search(q, bairro, atividade, situacao, 0, 1000, null, null, null);
        byte[] pdf = reportService.generatePdf(page.getContent());
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"contribuintes.pdf\"");
        response.setHeader("Content-Length", String.valueOf(pdf.length));
        try (java.io.OutputStream os = response.getOutputStream()) {
            os.write(pdf);
        }
    }

    @GetMapping("/contribuintes/excel")
    public void exportExcel(jakarta.servlet.http.HttpServletResponse response,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false) String bairro,
                            @RequestParam(required = false) String atividade,
                            @RequestParam(required = false) String situacao) throws java.io.IOException {
        Page<Contribuinte> page = service.search(q, bairro, atividade, situacao, 0, 1000, null, null, null);
        byte[] bytes = reportService.generateExcel(page.getContent());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"contribuintes.xlsx\"");
        response.setHeader("Content-Length", String.valueOf(bytes.length));
        try (java.io.OutputStream os = response.getOutputStream()) {
            os.write(bytes);
        }
    }
}
