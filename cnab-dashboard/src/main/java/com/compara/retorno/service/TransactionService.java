package com.compara.retorno.service;

import com.compara.retorno.model.Transacao;
import com.compara.retorno.model.TipoOrigem;
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
    public List<String> getAllFileSources() {
        return repository.findDistinctFileSources();
    }

    @Transactional
    public void deleteByFileSources(List<String> fileSources) {
        if (fileSources != null && !fileSources.isEmpty()) {
            repository.deleteByFileSourceIn(fileSources);
        }
    }

    public boolean isFileAlreadyImported(String fileName) {
        return repository.existsByFileSource(fileName);
    }

    @Transactional
    public void performReconciliation() {
        List<Transacao> apiTransactions = repository.findByTipoOrigem(TipoOrigem.API);

        for (Transacao api : apiTransactions) {
            // Find match in GERAL
            List<Transacao> matches = repository.findMatch(TipoOrigem.GERAL, api.getNossoNumero(), api.getValorPago(),
                    api.getDataOcorrencia());

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

        if (api == null)
            api = BigDecimal.ZERO;
        if (geral == null)
            geral = BigDecimal.ZERO;

        stats.setTotalApi(api);
        stats.setTotalGeral(geral);
        stats.setDifference(api.subtract(geral));

        stats.setCountDivergencias(countDiv);
        stats.setCountConciliados(countConc);

        // Financial Distribution
        TransacaoRepository.FinancialStats fStats = useCreditDate
                ? repository.getFinancialStatsByCredit(startDate, endDate)
                : repository.getFinancialStats(startDate, endDate);

        java.util.Map<String, BigDecimal> dist = new java.util.LinkedHashMap<>(); // LinkedHashMap to preserve order
        if (fStats != null) {
            dist.put("Abatimento", fStats.getSumAbatimento() != null ? fStats.getSumAbatimento() : BigDecimal.ZERO);
            dist.put("Desconto", fStats.getSumDesconto() != null ? fStats.getSumDesconto() : BigDecimal.ZERO);
            dist.put("IOF", fStats.getSumIof() != null ? fStats.getSumIof() : BigDecimal.ZERO);
            dist.put("Juros/Multa", fStats.getSumJurosMulta() != null ? fStats.getSumJurosMulta() : BigDecimal.ZERO);
            dist.put("Outras Despesas",
                    fStats.getSumOutrasDespesas() != null ? fStats.getSumOutrasDespesas() : BigDecimal.ZERO);
            dist.put("Outros Créditos",
                    fStats.getSumOutrosCreditos() != null ? fStats.getSumOutrosCreditos() : BigDecimal.ZERO);
            dist.put("Valor Líquido",
                    fStats.getSumValorLiquido() != null ? fStats.getSumValorLiquido() : BigDecimal.ZERO);
            dist.put("Valor Pago", fStats.getSumValorPago() != null ? fStats.getSumValorPago() : BigDecimal.ZERO);
            dist.put("Valor Tarifa", fStats.getSumValorTarifa() != null ? fStats.getSumValorTarifa() : BigDecimal.ZERO);
            dist.put("Valor Título", fStats.getSumValorTitulo() != null ? fStats.getSumValorTitulo() : BigDecimal.ZERO);
        }
        stats.setFinancialDistribution(dist);

        return stats;
    }

    public org.springframework.data.domain.Page<Transacao> findByPeriod(java.time.LocalDate start,
            java.time.LocalDate end, org.springframework.data.domain.Pageable pageable) {
        return repository.findByDataOcorrenciaBetween(start, end, pageable);
    }

    public List<Transacao> findAllByPeriod(java.time.LocalDate start, java.time.LocalDate end) {
        return repository.findByDataOcorrenciaBetween(start, end);
    }

    public org.springframework.data.domain.Page<Transacao> searchTransactions(String nomePagador, BigDecimal valorPago,
            org.springframework.data.domain.Pageable pageable) {
        return repository.findByNomePagadorAndValorPago(nomePagador, valorPago, pageable);
    }

    public List<Transacao> searchTransactionsList(String nomePagador, BigDecimal valorPago) {
        return repository.findByNomePagadorAndValorPagoList(nomePagador, valorPago);
    }

    // --- Advanced Comparison Logic ---

    public List<ComparisonResult> compareTransactions(java.time.LocalDate start, java.time.LocalDate end,
            boolean filterByCreditDate, boolean onlyDivergences) {
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
            Transacao api = group.stream().filter(t -> TipoOrigem.API == t.getTipoOrigem()).findFirst().orElse(null);
            Transacao geral = group.stream().filter(t -> TipoOrigem.GERAL == t.getTipoOrigem()).findFirst()
                    .orElse(null);

            ComparisonResult result = new ComparisonResult();
            result.setNossoNumero(entry.getKey());
            result.setApiTransaction(api);
            result.setGeralTransaction(geral);

            analyzeDiscrepancies(result);

            if (onlyDivergences && !result.isDivergent()) {
                continue;
            }

            results.add(result);
        }

        // Sort by discrepancies first, then date
        results.sort((a, b) -> {
            if (a.isDivergent() && !b.isDivergent())
                return -1;
            if (!a.isDivergent() && b.isDivergent())
                return 1;
            return 0;
        });

        return results;
    }

    private void analyzeDiscrepancies(ComparisonResult result) {
        List<String> logs = new java.util.ArrayList<>();
        boolean divergent = false;

        Transacao api = result.getApiTransaction();
        Transacao geral = result.getGeralTransaction();

        if (api == null && geral == null)
            return; // Should not happen

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
                logs.add(String.format("DIVERGÊNCIA DE VALOR: API[R$ %s] vs GERAL[R$ %s]", api.getValorPago(),
                        geral.getValorPago()));
                divergent = true;
            }

            // 2. Compare Payment Date
            if (!api.getDataOcorrencia().isEqual(geral.getDataOcorrencia())) {
                logs.add(String.format("DIVERGÊNCIA DATA PAGAMENTO: API[%s] vs GERAL[%s]", api.getDataOcorrencia(),
                        geral.getDataOcorrencia()));
                divergent = true;
            }

            // 3. Compare Credit Date (if present)
            if (api.getDataCredito() != null && geral.getDataCredito() != null) {
                if (!api.getDataCredito().isEqual(geral.getDataCredito())) {
                    logs.add(String.format("DIVERGÊNCIA DATA CRÉDITO: API[%s] vs GERAL[%s]", api.getDataCredito(),
                            geral.getDataCredito()));
                    divergent = true;
                }
            }

            result.setStatus(divergent ? "DIVERGENTE" : "CONCILIADO");
        }

        result.setDivergent(divergent);
        result.setLogs(logs);
    }

    public static class ComparisonResult {
        private String nossoNumero;
        private Transacao apiTransaction;
        private Transacao geralTransaction;
        private boolean divergent;
        private String status;
        private List<String> logs;

        // Helper methods for view
        public String getNomePagador() {
            if (apiTransaction != null)
                return apiTransaction.getNomePagador();
            if (geralTransaction != null)
                return geralTransaction.getNomePagador();
            return "?";
        }

        public java.time.LocalDate getMainDate() {
            if (apiTransaction != null)
                return apiTransaction.getDataOcorrencia();
            if (geralTransaction != null)
                return geralTransaction.getDataOcorrencia();
            return null;
        }

        public java.math.BigDecimal getValorPago() {
            if (apiTransaction != null)
                return apiTransaction.getValorPago();
            if (geralTransaction != null)
                return geralTransaction.getValorPago();
            return java.math.BigDecimal.ZERO;
        }

        public java.time.LocalDate getDataCredito() {
            if (apiTransaction != null)
                return apiTransaction.getDataCredito();
            if (geralTransaction != null)
                return geralTransaction.getDataCredito();
            return null;
        }

        // Getters and Setters
        public String getNossoNumero() {
            return nossoNumero;
        }

        public void setNossoNumero(String nossoNumero) {
            this.nossoNumero = nossoNumero;
        }

        public Transacao getApiTransaction() {
            return apiTransaction;
        }

        public void setApiTransaction(Transacao apiTransaction) {
            this.apiTransaction = apiTransaction;
        }

        public Transacao getGeralTransaction() {
            return geralTransaction;
        }

        public void setGeralTransaction(Transacao geralTransaction) {
            this.geralTransaction = geralTransaction;
        }

        public boolean isDivergent() {
            return divergent;
        }

        public void setDivergent(boolean divergent) {
            this.divergent = divergent;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getLogs() {
            return logs;
        }

        public void setLogs(List<String> logs) {
            this.logs = logs;
        }
    }

    public static class DashboardStats {
        private BigDecimal totalApi;
        private BigDecimal totalGeral;
        private BigDecimal difference;
        private Long countDivergencias;
        private Long countConciliados;
        private java.util.Map<String, BigDecimal> financialDistribution;

        // Getters and Setters
        public BigDecimal getTotalApi() {
            return totalApi;
        }

        public void setTotalApi(BigDecimal totalApi) {
            this.totalApi = totalApi;
        }

        public BigDecimal getTotalGeral() {
            return totalGeral;
        }

        public void setTotalGeral(BigDecimal totalGeral) {
            this.totalGeral = totalGeral;
        }

        public BigDecimal getDifference() {
            return difference;
        }

        public void setDifference(BigDecimal difference) {
            this.difference = difference;
        }

        public Long getCountDivergencias() {
            return countDivergencias;
        }

        public void setCountDivergencias(Long countDivergencias) {
            this.countDivergencias = countDivergencias;
        }

        public Long getCountConciliados() {
            return countConciliados;
        }

        public void setCountConciliados(Long countConciliados) {
            this.countConciliados = countConciliados;
        }

        public java.util.Map<String, BigDecimal> getFinancialDistribution() {
            return financialDistribution;
        }

        public void setFinancialDistribution(java.util.Map<String, BigDecimal> financialDistribution) {
            this.financialDistribution = financialDistribution;
        }
    }
}
