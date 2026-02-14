package com.compara.retorno.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "contribuintes", indexes = {
    @Index(name = "idx_contrib_cpf_cnpj", columnList = "cpfCnpj"),
    @Index(name = "idx_contrib_inscricao", columnList = "inscricaoMunicipal"),
    @Index(name = "idx_contrib_nome", columnList = "nome"),
    @Index(name = "idx_contrib_bairro", columnList = "bairro"),
    @Index(name = "idx_contrib_atividade", columnList = "atividadeEconomica"),
    @Index(name = "idx_contrib_situacao", columnList = "situacaoCadastral")
})
public class Contribuinte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 14)
    private String cpfCnpj; // somente dígitos

    @Column(nullable = false)
    private String nome;

    @Column(length = 30)
    private String inscricaoMunicipal;

    // Endereço
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;

    // Cadastro
    private String atividadeEconomica; // CNAE ou descrição
    @Column(nullable = false)
    private String situacaoCadastral; // ATIVO, SUSPENSO, BAIXA...
    private LocalDate dataAbertura;
    private LocalDate dataSituacao;

    @OneToMany(mappedBy = "contribuinte", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Pagamento> pagamentos;

    public Contribuinte() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getInscricaoMunicipal() { return inscricaoMunicipal; }
    public void setInscricaoMunicipal(String inscricaoMunicipal) { this.inscricaoMunicipal = inscricaoMunicipal; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getAtividadeEconomica() { return atividadeEconomica; }
    public void setAtividadeEconomica(String atividadeEconomica) { this.atividadeEconomica = atividadeEconomica; }

    public String getSituacaoCadastral() { return situacaoCadastral; }
    public void setSituacaoCadastral(String situacaoCadastral) { this.situacaoCadastral = situacaoCadastral; }

    public LocalDate getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDate dataAbertura) { this.dataAbertura = dataAbertura; }

    public LocalDate getDataSituacao() { return dataSituacao; }
    public void setDataSituacao(LocalDate dataSituacao) { this.dataSituacao = dataSituacao; }

    public List<Pagamento> getPagamentos() { return pagamentos; }
    public void setPagamentos(List<Pagamento> pagamentos) { this.pagamentos = pagamentos; }
}

