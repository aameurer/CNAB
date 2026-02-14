package com.compara.retorno.service;

import com.compara.retorno.model.AuditoriaConsulta;
import com.compara.retorno.model.Contribuinte;
import com.compara.retorno.repository.AuditoriaConsultaRepository;
import com.compara.retorno.repository.ContribuinteRepository;
import com.compara.retorno.repository.PagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ContribuinteService {

    @Autowired
    private ContribuinteRepository contribuinteRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private AuditoriaConsultaRepository auditoriaConsultaRepository;

    public Page<Contribuinte> search(String q, String bairro, String atividade, String situacao, int page, int size, String usuario, String ip, String userAgent) {
        long t0 = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size);
        Page<Contribuinte> result = contribuinteRepository.search(normalizeQuery(q), normalize(bairro), normalize(atividade), normalize(situacao), pageable);
        long took = System.currentTimeMillis() - t0;

        AuditoriaConsulta log = new AuditoriaConsulta();
        log.setUsuario(usuario);
        log.setTermoConsulta(safe(q));
        log.setFiltros(String.format("bairro=%s;atividade=%s;situacao=%s", safe(bairro), safe(atividade), safe(situacao)));
        log.setDuracaoMs(took);
        log.setTotalResultados((int) result.getTotalElements());
        log.setIp(ip);
        log.setUserAgent(userAgent);
        auditoriaConsultaRepository.save(log);

        return result;
    }

    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String normalizeQuery(String q) {
        if (q == null) return null;
        q = q.trim();
        if (q.isEmpty()) return null;
        String digits = q.replaceAll("\\D", "");
        if (digits.length() == 11 || digits.length() == 14) {
            return digits;
        }
        return q;
    }
}

