package com.compara.retorno.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "transacoes", indexes = {
    @Index(name = "idx_nosso_numero", columnList = "nossoNumero"),
    @Index(name = "idx_data_ocorrencia", columnList = "dataOcorrencia")
})
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipoOrigem; // API ou GERAL

    private String banco;
    private String lote;
    private String tipoRegistro;
    private String nSeq;
    private String segmento;
    private String movimento;
    private String agencia;
    private String conta;
    
    @Column(nullable = false)
    private String nossoNumero;
    
    private String carteira;
    private String numeroDocumento;
    private LocalDate vencimento;
    private BigDecimal valorTitulo;
    private String bancoCobrador;
    private String agenciaCobradora;
    private String idTituloEmpresa;
    private String tipoInscricao;
    private String numInscricao;
    private String nomePagador;
    private String numContrato;
    private BigDecimal valorTarifa;
    private String motivoOcorrencia;
    private BigDecimal jurosMulta;
    private BigDecimal desconto;
    private BigDecimal abatimento;
    private BigDecimal iof;
    
    @Column(nullable = false)
    private BigDecimal valorPago;
    
    private BigDecimal valorLiquido;
    private BigDecimal outrasDespesas;
    private BigDecimal outrosCreditos;
    
    @Column(nullable = false)
    private LocalDate dataOcorrencia;
    
    private LocalDate dataCredito;
    private String fileSource;
    
    // Status para controle do dashboard
    private String statusConciliacao; // PENDENTE, CONCILIADO, DIVERGENTE

    @Transient
    public Long getLagDays() {
        if (dataOcorrencia != null && dataCredito != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(dataOcorrencia, dataCredito);
        }
        return null;
    }
}
