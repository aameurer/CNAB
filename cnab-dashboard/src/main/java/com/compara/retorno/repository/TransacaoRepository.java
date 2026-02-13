package com.compara.retorno.repository;

import com.compara.retorno.model.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    
    List<Transacao> findByTipoOrigem(String tipoOrigem);
    Page<Transacao> findByTipoOrigem(String tipoOrigem, Pageable pageable);
    
    // Para comparação (encontrar correspondente)
    // Tenta encontrar uma transação do OUTRO tipo com mesmo nossoNumero, Valor e Data
    @Query("SELECT t FROM Transacao t WHERE t.tipoOrigem = :tipoOrigem AND t.nossoNumero = :nossoNumero AND t.valorPago = :valorPago AND t.dataOcorrencia = :dataOcorrencia")
    List<Transacao> findMatch(String tipoOrigem, String nossoNumero, BigDecimal valorPago, java.time.LocalDate dataOcorrencia);

    // Queries para Dashboard
    @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = 'API' AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
    BigDecimal sumValorApi(java.time.LocalDate startDate, java.time.LocalDate endDate);

    @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = 'GERAL' AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
    BigDecimal sumValorGeral(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'DIVERGENTE' AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
    Long countDivergencias(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'CONCILIADO' AND t.dataOcorrencia BETWEEN :startDate AND :endDate")
    Long countConciliados(java.time.LocalDate startDate, java.time.LocalDate endDate);

    // Queries para Dashboard (By Credit Date)
    @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = 'API' AND t.dataCredito BETWEEN :startDate AND :endDate")
    BigDecimal sumValorApiByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);

    @Query("SELECT SUM(t.valorPago) FROM Transacao t WHERE t.tipoOrigem = 'GERAL' AND t.dataCredito BETWEEN :startDate AND :endDate")
    BigDecimal sumValorGeralByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'DIVERGENTE' AND t.dataCredito BETWEEN :startDate AND :endDate")
    Long countDivergenciasByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    @Query("SELECT COUNT(t) FROM Transacao t WHERE t.statusConciliacao = 'CONCILIADO' AND t.dataCredito BETWEEN :startDate AND :endDate")
    Long countConciliadosByCredit(java.time.LocalDate startDate, java.time.LocalDate endDate);

    // Filtros
    Page<Transacao> findByStatusConciliacaoAndDataOcorrenciaBetween(String status, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    Page<Transacao> findByTipoOrigemAndDataOcorrenciaBetween(String tipoOrigem, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    Page<Transacao> findByDataOcorrenciaBetween(java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    
    // Filtros (By Credit Date)
    Page<Transacao> findByStatusConciliacaoAndDataCreditoBetween(String status, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    Page<Transacao> findByTipoOrigemAndDataCreditoBetween(String tipoOrigem, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    Page<Transacao> findByDataCreditoBetween(java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    
    // Date Range
    Page<Transacao> findByDataOcorrenciaBetween(java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    List<Transacao> findByDataOcorrenciaBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    // Credit Date Range
    List<Transacao> findByDataCreditoBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);
}
