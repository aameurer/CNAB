package com.compara.retorno.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public String handleAny(Throwable ex, HttpServletRequest request, Model model) {
        String uri = request.getRequestURI();
        model.addAttribute("error", "Ocorreu um erro ao processar a página. " +
                "Por favor, tente novamente. Detalhe: " + ex.getClass().getSimpleName());
        // Garantir atributos mínimos esperados pelos templates
        model.addAttribute("activePage", uri != null && uri.contains("contribuintes") ? "contribuintes" : "dashboard");
        model.addAttribute("results", org.springframework.data.domain.Page.empty());
        model.addAttribute("transactions", org.springframework.data.domain.Page.empty());
        model.addAttribute("filter", null);
        model.addAttribute("startDate", null);
        model.addAttribute("endDate", null);
        model.addAttribute("useCreditDate", false);
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalApi", java.math.BigDecimal.ZERO);
        stats.put("totalGeral", java.math.BigDecimal.ZERO);
        stats.put("difference", java.math.BigDecimal.ZERO);
        stats.put("countConciliados", 0);
        stats.put("countDivergencias", 0);
        stats.put("financialDistribution", java.util.Collections.emptyMap());
        model.addAttribute("took", 0);
        model.addAttribute("stats", stats);
        // Escolhe uma view adequada conforme a rota
        if (uri != null && uri.startsWith("/contribuintes")) {
            return "contribuintes";
        }
        return "dashboard";
    }
}
