package com.compara.retorno.service;

import com.compara.retorno.model.Transacao;
import com.compara.retorno.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransacaoRepository repository;

    @Transactional
    public void saveAll(List<Transacao> transactions) {
        repository.saveAll(transactions);
    }

    @Transactional
    public void clearAll() {
        repository.deleteAll();
    }
    
    @Transactional
    public void performReconciliation() {
        // Logic: Iterate over API transactions and find matching GERAL transactions
        // Or pure SQL update?
        // Let's keep it simple: Reset status first
        
        // This is a naive approach, for huge datasets we should use stored procedures or batch updates
        List<Transacao> apiTransactions = repository.findByTipoOrigem("API");
        
        for (Transacao api : apiTransactions) {
            // Find match in GERAL
            List<Transacao> matches = repository.findMatch("GERAL", api.getNossoNumero(), api.getValorPago(), api.getDataOcorrencia());
            
            if (!matches.isEmpty()) {
                api.setStatusConciliacao("CONCILIADO");
                for (Transacao m : matches) {
                    m.setStatusConciliacao("CONCILIADO");
                }
                repository.save(api);
                repository.saveAll(matches);
            } else {
                api.setStatusConciliacao("DIVERGENTE");
                repository.save(api);
            }
        }
        
        // Mark remaining GERAL as DIVERGENTE (if not CONCILIADO)
        // We can do this via query
        // "UPDATE transacoes SET status_conciliacao = 'DIVERGENTE' WHERE tipo_origem = 'GERAL' AND status_conciliacao = 'PENDENTE'"
        // But let's stick to JPA for now or add a custom query
    }
    
    public DashboardStats getStats(java.time.LocalDate startDate, java.time.LocalDate endDate, boolean useCreditDate) {
        DashboardStats stats = new DashboardStats();
        BigDecimal api, geral;
        Long countDiv, countConc;
        
        if (useCreditDate) {
            api = repository.sumValorApiByCredit(startDate, endDate);
            geral = repository.sumValorGeralByCredit(startDate, endDate);
            countDiv = repository.countDivergenciasByCredit(startDate, endDate);
            countConc = repository.countConciliadosByCredit(startDate, endDate);
        } else {
            api = repository.sumValorApi(startDate, endDate);
            geral = repository.sumValorGeral(startDate, endDate);
            countDiv = repository.countDivergencias(startDate, endDate);
            countConc = repository.countConciliados(startDate, endDate);
        }
        
        if (api == null) api = BigDecimal.ZERO;
        if (geral == null) geral = BigDecimal.ZERO;
        
        stats.setTotalApi(api);
        stats.setTotalGeral(geral);
        stats.setDifference(api.subtract(geral));
        
        stats.setCountDivergencias(countDiv);
        stats.setCountConciliados(countConc);
        return stats;
    }
    
    public org.springframework.data.domain.Page<Transacao> findByPeriod(java.time.LocalDate start, java.time.LocalDate end, org.springframework.data.domain.Pageable pageable) {
        return repository.findByDataOcorrenciaBetween(start, end, pageable);
    }

    public List<Transacao> findAllByPeriod(java.time.LocalDate start, java.time.LocalDate end) {
        return repository.findByDataOcorrenciaBetween(start, end);
    }
    
    // --- Advanced Comparison Logic ---
    
    public List<ComparisonResult> compareTransactions(java.time.LocalDate start, java.time.LocalDate end, boolean filterByCreditDate) {
        List<Transacao> allTransactions;
        
        if (filterByCreditDate) {
            allTransactions = repository.findByDataCreditoBetween(start, end);
        } else {
            allTransactions = repository.findByDataOcorrenciaBetween(start, end);
        }
        
        // Group by 'Nosso Numero' to find pairs
        java.util.Map<String, List<Transacao>> grouped = allTransactions.stream()
            .collect(java.util.stream.Collectors.groupingBy(Transacao::getNossoNumero));
            
        List<ComparisonResult> results = new java.util.ArrayList<>();
        
        for (java.util.Map.Entry<String, List<Transacao>> entry : grouped.entrySet()) {
            List<Transacao> group = entry.getValue();
            Transacao api = group.stream().filter(t -> "API".equals(t.getTipoOrigem())).findFirst().orElse(null);
            Transacao geral = group.stream().filter(t -> "GERAL".equals(t.getTipoOrigem())).findFirst().orElse(null);
            
            ComparisonResult result = new ComparisonResult();
            result.setNossoNumero(entry.getKey());
            result.setApiTransaction(api);
            result.setGeralTransaction(geral);
            
            analyzeDiscrepancies(result);
            results.add(result);
        }
        
        // Sort by discrepancies first, then date
        results.sort((a, b) -> {
            if (a.isDivergent() && !b.isDivergent()) return -1;
            if (!a.isDivergent() && b.isDivergent()) return 1;
            return 0; 
        });
        
        return results;
    }
    
    private void analyzeDiscrepancies(ComparisonResult result) {
        List<String> logs = new java.util.ArrayList<>();
        boolean divergent = false;
        
        Transacao api = result.getApiTransaction();
        Transacao geral = result.getGeralTransaction();
        
        if (api == null && geral == null) return; // Should not happen
        
        if (api == null) {
            logs.add("AUSENTE NA API: Registro encontrado apenas no arquivo GERAL.");
            result.setStatus("SOMENTE_GERAL");
            divergent = true;
        } else if (geral == null) {
            logs.add("AUSENTE NO GERAL: Registro encontrado apenas na API.");
            result.setStatus("SOMENTE_API");
            divergent = true;
        } else {
            // Both exist - Deep Comparison
            
            // 1. Compare Value
            if (api.getValorPago().compareTo(geral.getValorPago()) != 0) {
                logs.add(String.format("DIVERGÊNCIA DE VALOR: API[R$ %s] vs GERAL[R$ %s]", api.getValorPago(), geral.getValorPago()));
                divergent = true;
            }
            
            // 2. Compare Payment Date
            if (!api.getDataOcorrencia().isEqual(geral.getDataOcorrencia())) {
                logs.add(String.format("DIVERGÊNCIA DATA PAGAMENTO: API[%s] vs GERAL[%s]", api.getDataOcorrencia(), geral.getDataOcorrencia()));
                divergent = true;
            }
            
            // 3. Compare Credit Date (if present)
            if (api.getDataCredito() != null && geral.getDataCredito() != null) {
                if (!api.getDataCredito().isEqual(geral.getDataCredito())) {
                    logs.add(String.format("DIVERGÊNCIA DATA CRÉDITO: API[%s] vs GERAL[%s]", api.getDataCredito(), geral.getDataCredito()));
                    divergent = true;
                }
            }
            
            result.setStatus(divergent ? "DIVERGENTE" : "CONCILIADO");
        }
        
        result.setDivergent(divergent);
        result.setLogs(logs);
    }
    
    @lombok.Data
    public static class ComparisonResult {
        private String nossoNumero;
        private Transacao apiTransaction;
        private Transacao geralTransaction;
        private boolean divergent;
        private String status;
        private List<String> logs;
        
        // Helper methods for view
        public String getNomePagador() {
            if (apiTransaction != null) return apiTransaction.getNomePagador();
            if (geralTransaction != null) return geralTransaction.getNomePagador();
            return "?";
        }
        
        public java.time.LocalDate getMainDate() {
             if (apiTransaction != null) return apiTransaction.getDataOcorrencia();
             if (geralTransaction != null) return geralTransaction.getDataOcorrencia();
             return null;
        }
    }

    @lombok.Data
    public static class DashboardStats {
        private BigDecimal totalApi;
        private BigDecimal totalGeral;
        private BigDecimal difference;
        private Long countDivergencias;
        private Long countConciliados;
    }
}
