package com.feira.conecta.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Demanda;
import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.repository.DemandaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável pela lógica de matching entre Anúncios e Demandas.
 *
 * <p>Estratégia de matching: ao salvar um novo Anúncio, buscamos todas as
 * Demandas com status PROCURANDO que tenham o mesmo Produto. Para cada match
 * encontrado, notificamos ambos os usuários de forma assíncrona, sem bloquear
 * a requisição original de criação do anúncio.</p>
 *
 * <p>O método é anotado com {@code @Async} — para funcionar é necessário que
 * a classe de configuração principal (ou qualquer {@code @Configuration}) tenha
 * a anotação {@code @EnableAsync}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnuncioMatchingService {

    private final DemandaRepository demandaRepository;
    private final NotificacaoService notificacaoService;

    /**
     * Executa a busca por demandas compatíveis com o anúncio recém-criado.
     *
     * <p>Matching por: <strong>produto</strong> (campo obrigatório).
     * O método roda em thread separada ({@code @Async}) para não atrasar
     * a resposta HTTP do endpoint de criação do anúncio.</p>
     *
     * @param anuncio o anúncio que acabou de ser persistido (com ID gerado)
     */
    @Async
    @Transactional
    public void executarMatching(Anuncio anuncio) {
        log.info("Iniciando matching assíncrono para Anúncio id={}, Produto='{}'",
                anuncio.getId(), anuncio.getProduto().getNome());

        List<Demanda> demandasCompatíveis = demandaRepository
                .findByProdutoAndStatus(anuncio.getProduto(), StatusDemanda.PROCURANDO);

        if (demandasCompatíveis.isEmpty()) {
            log.info("Nenhuma demanda compatível encontrada para Anúncio id={}", anuncio.getId());
            return;
        }

        log.info("{} demanda(s) compatível(is) encontrada(s) para Anúncio id={}",
                demandasCompatíveis.size(), anuncio.getId());

        for (Demanda demanda : demandasCompatíveis) {
            notificarMatch(anuncio, demanda);
        }
    }

    /**
     * Gera notificações para o vendedor (dono do anúncio) e para o comprador
     * (dono da demanda) quando um match é identificado.
     */
    private void notificarMatch(Anuncio anuncio, Demanda demanda) {
        String nomeProduto = anuncio.getProduto().getNome();

        // Notificação para o VENDEDOR (dono do anúncio)
        String mensagemVendedor = String.format(
                "🎯 Match encontrado! Seu anúncio de '%s' (id=%d) corresponde à demanda de %s (id=%d). Entre em contato!",
                nomeProduto,
                anuncio.getId(),
                demanda.getComprador().getNome(),
                demanda.getId()
        );
        notificacaoService.criar(anuncio.getUsuario(), mensagemVendedor);

        // Notificação para o COMPRADOR (dono da demanda)
        String mensagemComprador = String.format(
                "🎯 Match encontrado! Sua demanda por '%s' (id=%d) tem um anúncio disponível de %s (id=%d). Confira!",
                nomeProduto,
                demanda.getId(),
                anuncio.getUsuario().getNome(),
                anuncio.getId()
        );
        notificacaoService.criar(demanda.getComprador(), mensagemComprador);

        log.info("Notificações de match geradas: Vendedor id={} <-> Comprador id={}",
                anuncio.getUsuario().getId(), demanda.getComprador().getId());
    }
}