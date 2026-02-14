package com.compara.retorno.repository;

import com.compara.retorno.model.Contribuinte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContribuinteRepository extends JpaRepository<Contribuinte, Long> {

    @Query("""
        SELECT c FROM Contribuinte c
        WHERE (:q IS NULL OR :q = '' OR 
              c.cpfCnpj LIKE %:q% OR 
              LOWER(c.nome) LIKE LOWER(CONCAT('%', :q, '%')) OR 
              c.inscricaoMunicipal LIKE %:q%)
          AND (:bairro IS NULL OR :bairro = '' OR LOWER(c.bairro) = LOWER(:bairro))
          AND (:atividade IS NULL OR :atividade = '' OR LOWER(c.atividadeEconomica) = LOWER(:atividade))
          AND (:situacao IS NULL OR :situacao = '' OR c.situacaoCadastral = :situacao)
        ORDER BY c.nome ASC
    """)
    Page<Contribuinte> search(
            @Param("q") String q,
            @Param("bairro") String bairro,
            @Param("atividade") String atividade,
            @Param("situacao") String situacao,
            Pageable pageable);
}

