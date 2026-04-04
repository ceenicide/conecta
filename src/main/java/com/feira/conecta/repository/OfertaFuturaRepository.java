// repository/OfertaFuturaRepository.java
package com.feira.conecta.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusOferta;

@Repository
public interface OfertaFuturaRepository extends JpaRepository<OfertaFutura, Long> {
    List<OfertaFutura> findByStatus(StatusOferta status);
    List<OfertaFutura> findByUsuarioId(Long usuarioId);
    List<OfertaFutura> findByProdutoAndStatusAndDataDisponivelLessThanEqual(
        Produto produto, StatusOferta status, LocalDate dataLimite);
}