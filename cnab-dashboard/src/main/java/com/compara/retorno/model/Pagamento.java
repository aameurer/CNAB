package com.compara.retorno.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagamentos", indexes = {
    @Index(name = "idx_pag_contribuinte", columnList = "contribuinte_id"),
    @Index(name = "idx_pag_data", columnList = "dataPagamento")
})
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contribuinte_id", nullable = false)
    private Contribuinte contribuinte;

    private LocalDate dataPagamento;
    private String referencia; // competência/tributo
    private BigDecimal valor;
    private String forma; // opcional

    public Pagamento() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Contribuinte getContribuinte() { return contribuinte; }
    public void setContribuinte(Contribuinte contribuinte) { this.contribuinte = contribuinte; }

    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getForma() { return forma; }
    public void setForma(String forma) { this.forma = forma; }
}

