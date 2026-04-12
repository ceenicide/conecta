// repository/DemandaRepository.java
package com.feira.conecta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.feira.conecta.domain.Demanda;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusDemanda;

@Repository
public interface DemandaRepository extends JpaRepository<Demanda, Long> {
    List<Demanda> findByStatus(StatusDemanda status);
    List<Demanda> findByCompradorId(Long compradorId);
    List<Demanda> findByProdutoAndStatus(Produto produto, StatusDemanda status);
}