package com.compara.retorno.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "transacoes", indexes = {
    @Index(name = "idx_nosso_numero", columnList = "nossoNumero"),
    @Index(name = "idx_data_ocorrencia", columnList = "dataOcorrencia")
})
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOrigem tipoOrigem; // API ou GERAL

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

    public Transacao() {}

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoOrigem getTipoOrigem() { return tipoOrigem; }
    public void setTipoOrigem(TipoOrigem tipoOrigem) { this.tipoOrigem = tipoOrigem; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public String getTipoRegistro() { return tipoRegistro; }
    public void setTipoRegistro(String tipoRegistro) { this.tipoRegistro = tipoRegistro; }

    public String getNSeq() { return nSeq; }
    public void setNSeq(String nSeq) { this.nSeq = nSeq; }

    public String getSegmento() { return segmento; }
    public void setSegmento(String segmento) { this.segmento = segmento; }

    public String getMovimento() { return movimento; }
    public void setMovimento(String movimento) { this.movimento = movimento; }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getConta() { return conta; }
    public void setConta(String conta) { this.conta = conta; }

    public String getNossoNumero() { return nossoNumero; }
    public void setNossoNumero(String nossoNumero) { this.nossoNumero = nossoNumero; }

    public String getCarteira() { return carteira; }
    public void setCarteira(String carteira) { this.carteira = carteira; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public BigDecimal getValorTitulo() { return valorTitulo; }
    public void setValorTitulo(BigDecimal valorTitulo) { this.valorTitulo = valorTitulo; }

    public String getBancoCobrador() { return bancoCobrador; }
    public void setBancoCobrador(String bancoCobrador) { this.bancoCobrador = bancoCobrador; }

    public String getAgenciaCobradora() { return agenciaCobradora; }
    public void setAgenciaCobradora(String agenciaCobradora) { this.agenciaCobradora = agenciaCobradora; }

    public String getIdTituloEmpresa() { return idTituloEmpresa; }
    public void setIdTituloEmpresa(String idTituloEmpresa) { this.idTituloEmpresa = idTituloEmpresa; }

    public String getTipoInscricao() { return tipoInscricao; }
    public void setTipoInscricao(String tipoInscricao) { this.tipoInscricao = tipoInscricao; }

    public String getNumInscricao() { return numInscricao; }
    public void setNumInscricao(String numInscricao) { this.numInscricao = numInscricao; }

    public String getNomePagador() { return nomePagador; }
    public void setNomePagador(String nomePagador) { this.nomePagador = nomePagador; }

    public String getNumContrato() { return numContrato; }
    public void setNumContrato(String numContrato) { this.numContrato = numContrato; }

    public BigDecimal getValorTarifa() { return valorTarifa; }
    public void setValorTarifa(BigDecimal valorTarifa) { this.valorTarifa = valorTarifa; }

    public String getMotivoOcorrencia() { return motivoOcorrencia; }
    public void setMotivoOcorrencia(String motivoOcorrencia) { this.motivoOcorrencia = motivoOcorrencia; }

    public BigDecimal getJurosMulta() { return jurosMulta; }
    public void setJurosMulta(BigDecimal jurosMulta) { this.jurosMulta = jurosMulta; }

    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }

    public BigDecimal getAbatimento() { return abatimento; }
    public void setAbatimento(BigDecimal abatimento) { this.abatimento = abatimento; }

    public BigDecimal getIof() { return iof; }
    public void setIof(BigDecimal iof) { this.iof = iof; }

    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }

    public BigDecimal getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(BigDecimal valorLiquido) { this.valorLiquido = valorLiquido; }

    public BigDecimal getOutrasDespesas() { return outrasDespesas; }
    public void setOutrasDespesas(BigDecimal outrasDespesas) { this.outrasDespesas = outrasDespesas; }

    public BigDecimal getOutrosCreditos() { return outrosCreditos; }
    public void setOutrosCreditos(BigDecimal outrosCreditos) { this.outrosCreditos = outrosCreditos; }

    public LocalDate getDataOcorrencia() { return dataOcorrencia; }
    public void setDataOcorrencia(LocalDate dataOcorrencia) { this.dataOcorrencia = dataOcorrencia; }

    public LocalDate getDataCredito() { return dataCredito; }
    public void setDataCredito(LocalDate dataCredito) { this.dataCredito = dataCredito; }

    public String getFileSource() { return fileSource; }
    public void setFileSource(String fileSource) { this.fileSource = fileSource; }

    public String getStatusConciliacao() { return statusConciliacao; }
    public void setStatusConciliacao(String statusConciliacao) { this.statusConciliacao = statusConciliacao; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transacao transacao = (Transacao) o;
        return Objects.equals(id, transacao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
