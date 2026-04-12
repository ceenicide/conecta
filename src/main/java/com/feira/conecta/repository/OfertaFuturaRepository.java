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

    // Usada pelo MatchingService — filtra por produto específico
    List<OfertaFutura> findByProdutoAndStatusAndDataDisponivelLessThanEqual(
            Produto produto, StatusOferta status, LocalDate dataLimite);

    // NOVA: usada por DemandaService.listarOfertasCompativeis
    // Retorna todas as ofertas abertas cuja dataDisponivel <= dataLimite fornecida
    // Não filtra por produto — o comprador vê todas as ofertas dentro do prazo,
    // independente do produto (visão ampla do mercado futuro)
    List<OfertaFutura> findByStatusAndDataDisponivelLessThanEqual(
            StatusOferta status, LocalDate dataLimite);
}