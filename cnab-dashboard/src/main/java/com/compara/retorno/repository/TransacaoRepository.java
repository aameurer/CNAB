package com.compara.retorno.repository;

import com.compara.retorno.model.Transacao;
import com.compara.retorno.model.TipoOrigem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

        List<Transacao> findByTipoOrigem(TipoOrigem tipoOrigem);

        @Query(value = "SELECT * FROM transacoes t WHERE " +
                        "(:nomePagador IS NULL OR t.nome_pagador ILIKE CONCAT('%', :nomePagador, '%')) AND " +
                        "(:valorPago IS NULL OR t.valor_pago = :valorPago)", countQuery = "SELECT count(*) FROM transacoes t WHERE "
                                        +
                                        "(:nomePagador IS NULL OR t.nome_pagador ILIKE CONCAT('%', :nomePagador, '%')) AND "
                                        +
                                        "(:valorPago IS NULL OR t.valor_pago = :valorPago)", nativeQuery = true)
        Page<Transacao> findByNomePagadorAndValorPago(
                        @org.springframework.data.repository.query.Param("nomePagador") String nomePagador,
                        @org.springframework.data.repository.query.Param("valorPago") BigDecimal valorPago,
                        Pageable pageable);

        @Query(value = "SELECT * FROM transacoes t WHERE " +
                        "(:nomePagador IS NULL OR t.nome_pagador ILIKE CONCAT('%', :nomePagador, '%')) AND " +
                        "(:valorPago IS NULL OR t.valor_pago = :valorPago) " +
                        "ORDER BY t.data_ocorrencia DESC", nativeQuery = true)
        List<Transacao> findByNomePagadorAndValorPagoList(
                        @org.springframework.data.repository.query.Param("nomePagador") String nomePagador,
                        @org.springframework.data.repository.query.Param("valorPago") BigDecimal valorPago);

        Page<Transacao> findByTipoOrigem(TipoOrigem tipoOrigem, Pageable pageable);

        // Para comparação (encontrar correspondente)
        // Tenta encontrar uma transação do OUTRO tipo com mesmo nossoNumero, Valor e
        // Data
        @Query("SELECT t FROM Transacao t WHERE t.tipoOrigem = :tipoOrigem AND t.nossoNumero = :nossoNumero AND t.valorPago = :valorPago AND t.dataOcorrencia = :dataOcorrencia")
        List<Transacao> findMatch(TipoOrigem tipoOrigem, String nossoNumero, BigDecimal valorPago,
                        java.time.LocalDate dataOcorrencia);

        // Queries para Dashboard
        @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = com.compara.retorno.model.TipoOrigem.API AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
        BigDecimal sumValorApi(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = com.compara.retorno.model.TipoOrigem.GERAL AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
        BigDecimal sumValorGeral(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'DIVERGENTE' AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
        Long countDivergencias(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'CONCILIADO' AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
        Long countConciliados(java.time.LocalDate startDate, java.time.LocalDate endDate);

        // Queries para Dashboard (By Credit Date)
        @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = com.compara.retorno.model.TipoOrigem.API AND t.dataCredito BETWEEN :startDate AND :endDate")
        BigDecimal sumValorApiByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = com.compara.retorno.model.TipoOrigem.GERAL AND t.dataCredito BETWEEN :startDate AND :endDate")
        BigDecimal sumValorGeralByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'DIVERGENTE' AND t.dataCredito BETWEEN :startDate AND :endDate")
        Long countDivergenciasByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'CONCILIADO' AND t.dataCredito BETWEEN :startDate AND :endDate")
        Long countConciliadosByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);

        // Filtros
        Page<Transacao> findByStatusConciliacaoAndDataOcorrenciaBetween(String status, java.time.LocalDate startDate,
                        java.time.LocalDate endDate, Pageable pageable);

        Page<Transacao> findByTipoOrigemAndDataOcorrenciaBetween(TipoOrigem tipoOrigem, java.time.LocalDate startDate,
                        java.time.LocalDate endDate, Pageable pageable);

        Page<Transacao> findByDataOcorrenciaBetween(java.time.LocalDate startDate, java.time.LocalDate endDate,
                        Pageable pageable);

        // Filtros (By Credit Date)
        Page<Transacao> findByStatusConciliacaoAndDataCreditoBetween(String status, java.time.LocalDate startDate,
                        java.time.LocalDate endDate, Pageable pageable);

        Page<Transacao> findByTipoOrigemAndDataCreditoBetween(TipoOrigem tipoOrigem, java.time.LocalDate startDate,
                        java.time.LocalDate endDate, Pageable pageable);

        Page<Transacao> findByDataCreditoBetween(java.time.LocalDate startDate, java.time.LocalDate endDate,
                        Pageable pageable);

        // Date Range
        List<Transacao> findByDataOcorrenciaBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

        // Credit Date Range
        List<Transacao> findByDataCreditoBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

        // File Management
        @Query("SELECT DISTINCT t.fileSource FROM Transacao t WHERE t.fileSource IS NOT NULL ORDER BY t.fileSource")
        List<String> findDistinctFileSources();

        void deleteByFileSourceIn(List<String> fileSources);

        boolean existsByFileSource(String fileSource);

        // Interface para projeção dos totais financeiros
        public interface FinancialStats {
                BigDecimal getSumAbatimento();

                BigDecimal getSumDesconto();

                BigDecimal getSumIof();

                BigDecimal getSumJurosMulta();

                BigDecimal getSumOutrasDespesas();

                BigDecimal getSumOutrosCreditos();

                BigDecimal getSumValorLiquido();

                BigDecimal getSumValorPago();

                BigDecimal getSumValorTarifa();

                BigDecimal getSumValorTitulo();
        }

        @Query("SELECT " +
                        "SUM(t.abatimento) as sumAbatimento, " +
                        "SUM(t.desconto) as sumDesconto, " +
                        "SUM(t.iof) as sumIof, " +
                        "SUM(t.jurosMulta) as sumJurosMulta, " +
                        "SUM(t.outrasDespesas) as sumOutrasDespesas, " +
                        "SUM(t.outrosCreditos) as sumOutrosCreditos, " +
                        "SUM(t.valorLiquido) as sumValorLiquido, " +
                        "SUM(t.valorPago) as sumValorPago, " +
                        "SUM(t.valorTarifa) as sumValorTarifa, " +
                        "SUM(t.valorTitulo) as sumValorTitulo " +
                        "FROM Transacao t WHERE t.tipoOrigem = com.compara.retorno.model.TipoOrigem.GERAL AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
        FinancialStats getFinancialStats(java.time.LocalDate startDate, java.time.LocalDate endDate);

        @Query("SELECT " +
                        "SUM(t.abatimento) as sumAbatimento, " +
                        "SUM(t.desconto) as sumDesconto, " +
                        "SUM(t.iof) as sumIof, " +
                        "SUM(t.jurosMulta) as sumJurosMulta, " +
                        "SUM(t.outrasDespesas) as sumOutrasDespesas, " +
                        "SUM(t.outrosCreditos) as sumOutrosCreditos, " +
                        "SUM(t.valorLiquido) as sumValorLiquido, " +
                        "SUM(t.valorPago) as sumValorPago, " +
                        "SUM(t.valorTarifa) as sumValorTarifa, " +
                        "SUM(t.valorTitulo) as sumValorTitulo " +
                        "FROM Transacao t WHERE t.tipoOrigem = com.compara.retorno.model.TipoOrigem.GERAL AND t.dataCredito BETWEEN :startDate AND :endDate")
        FinancialStats getFinancialStatsByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);
}
