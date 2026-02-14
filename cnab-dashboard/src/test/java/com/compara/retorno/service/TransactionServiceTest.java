package com.compara.retorno.service;

import com.compara.retorno.model.Transacao;
import com.compara.retorno.model.TipoOrigem;
import com.compara.retorno.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    @Mock
    private TransacaoRepository repository;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCompareTransactions_All() {
        // Arrange
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now();
        
        Transacao t1Api = createTransaction("001", TipoOrigem.API, new BigDecimal("100.00"), start);
        Transacao t1Geral = createTransaction("001", TipoOrigem.GERAL, new BigDecimal("100.00"), start); // Match
        
        Transacao t2Api = createTransaction("002", TipoOrigem.API, new BigDecimal("200.00"), start); // Missing GERAL (Divergent)
        
        when(repository.findByDataOcorrenciaBetween(any(), any())).thenReturn(Arrays.asList(t1Api, t1Geral, t2Api));
        
        // Act
        List<TransactionService.ComparisonResult> results = transactionService.compareTransactions(start, end, false, false);
        
        // Assert
        assertEquals(2, results.size());
        
        // Check first result (Divergent one should be first due to sort)
        assertTrue(results.stream().anyMatch(r -> r.getNossoNumero().equals("002") && r.isDivergent()));
        assertTrue(results.stream().anyMatch(r -> r.getNossoNumero().equals("001") && !r.isDivergent()));
    }

    @Test
    void testCompareTransactions_OnlyDivergences() {
        // Arrange
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now();
        
        Transacao t1Api = createTransaction("001", TipoOrigem.API, new BigDecimal("100.00"), start);
        Transacao t1Geral = createTransaction("001", TipoOrigem.GERAL, new BigDecimal("100.00"), start); // Match
        
        Transacao t2Api = createTransaction("002", TipoOrigem.API, new BigDecimal("200.00"), start); // Missing GERAL (Divergent)
        
        when(repository.findByDataOcorrenciaBetween(any(), any())).thenReturn(Arrays.asList(t1Api, t1Geral, t2Api));
        
        // Act
        List<TransactionService.ComparisonResult> results = transactionService.compareTransactions(start, end, false, true);
        
        // Assert
        assertEquals(1, results.size());
        assertEquals("002", results.get(0).getNossoNumero());
        assertTrue(results.get(0).isDivergent());
    }

    private Transacao createTransaction(String nossoNumero, TipoOrigem origem, BigDecimal valor, LocalDate data) {
        Transacao t = new Transacao();
        t.setNossoNumero(nossoNumero);
        t.setTipoOrigem(origem);
        t.setValorPago(valor);
        t.setDataOcorrencia(data);
        return t;
    }
}
