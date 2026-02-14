package com.compara.retorno.repository;

import com.compara.retorno.model.AuditoriaConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaConsultaRepository extends JpaRepository<AuditoriaConsulta, Long> {
}

