package com.compara.retorno.service;

import com.compara.retorno.model.AuditoriaConsulta;
import com.compara.retorno.model.Contribuinte;
import com.compara.retorno.repository.AuditoriaConsultaRepository;
import com.compara.retorno.repository.ContribuinteRepository;
import com.compara.retorno.repository.PagamentoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ContribuinteServiceLoadTest {

    @Test
    public void supportsThousandConcurrentSearchesUnderThreeSecondsEach() throws Exception {
        ContribuinteRepository contribRepo = Mockito.mock(ContribuinteRepository.class);
        PagamentoRepository pagRepo = Mockito.mock(PagamentoRepository.class);
        AuditoriaConsultaRepository audRepo = Mockito.mock(AuditoriaConsultaRepository.class);

        List<Contribuinte> sample = Collections.singletonList(new Contribuinte());
        Page<Contribuinte> samplePage = new PageImpl<>(sample, PageRequest.of(0, 10), 1);
        when(contribRepo.search(any(), any(), any(), any(), any())).thenReturn(samplePage);
        when(audRepo.save(Mockito.any(AuditoriaConsulta.class))).thenAnswer(i -> i.getArgument(0));

        ContribuinteService service = new ContribuinteService();
        setField(service, "contribuinteRepository", contribRepo);
        setField(service, "pagamentoRepository", pagRepo);
        setField(service, "auditoriaConsultaRepository", audRepo);

        int N = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(100);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < N; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                long t0 = System.currentTimeMillis();
                Page<Contribuinte> p = service.search("12345678901", null, null, "ATIVO", 0, 10, null, null, null);
                long took = System.currentTimeMillis() - t0;
                assertTrue(took < 3000, "cada busca deve durar menos de 3s");
                assertTrue(p.getTotalElements() >= 1);
            }, pool));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(totalMs < 30000, "todas as buscas devem concluir rápido o bastante");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}

