package com.compara.retorno.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_consulta", indexes = {
    @Index(name = "idx_auditoria_data", columnList = "dataHora")
})
public class AuditoriaConsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuario; // opcional
    @Column(length = 500)
    private String termoConsulta;
    @Column(length = 500)
    private String filtros;
    private Long duracaoMs;
    private Integer totalResultados;
    private String ip;
    private String userAgent;
    private LocalDateTime dataHora = LocalDateTime.now();

    public AuditoriaConsulta() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getTermoConsulta() { return termoConsulta; }
    public void setTermoConsulta(String termoConsulta) { this.termoConsulta = termoConsulta; }
    public String getFiltros() { return filtros; }
    public void setFiltros(String filtros) { this.filtros = filtros; }
    public Long getDuracaoMs() { return duracaoMs; }
    public void setDuracaoMs(Long duracaoMs) { this.duracaoMs = duracaoMs; }
    public Integer getTotalResultados() { return totalResultados; }
    public void setTotalResultados(Integer totalResultados) { this.totalResultados = totalResultados; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public java.time.LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(java.time.LocalDateTime dataHora) { this.dataHora = dataHora; }
}

