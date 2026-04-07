package com.feira.conecta.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Demanda;
import com.feira.conecta.domain.Matching;
import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.domain.StatusMatching;
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.MatchingDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.DemandaRepository;
import com.feira.conecta.repository.MatchingRepository;
import com.feira.conecta.repository.OfertaFuturaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRepository matchingRepository;
    private final OfertaFuturaRepository ofertaRepository;
    private final DemandaRepository demandaRepository;
    private final SecurityUtils securityUtils; //  FIX: Necessário para verificar ownership

    @Transactional
    public void buscarMatchesPorOferta(OfertaFutura oferta) {
        List<Demanda> demandasCompativeis = demandaRepository
                .findByProdutoAndStatus(oferta.getProduto(), StatusDemanda.PROCURANDO)
                .stream()
                .filter(d -> !oferta.getDataDisponivel().isAfter(d.getDataLimite()))
                .filter(d -> oferta.getQuantidade().compareTo(d.getQuantidade()) >= 0)
                .filter(d -> !matchingRepository.existsByOfertaIdAndDemandaId(oferta.getId(), d.getId()))
                .toList();

        demandasCompativeis.forEach(demanda -> {
            Matching matching = Matching.builder()
                    .oferta(oferta)
                    .demanda(demanda)
                    .status(StatusMatching.PENDENTE)
                    .build();
            matchingRepository.save(matching);
        });
    }

    @Transactional
    public void buscarMatchesPorDemanda(Demanda demanda) {
        List<OfertaFutura> ofertasCompativeis = ofertaRepository
                .findByProdutoAndStatusAndDataDisponivelLessThanEqual(
                        demanda.getProduto(), StatusOferta.ABERTA, demanda.getDataLimite())
                .stream()
                .filter(o -> o.getQuantidade().compareTo(demanda.getQuantidade()) >= 0)
                .filter(o -> !matchingRepository.existsByOfertaIdAndDemandaId(o.getId(), demanda.getId()))
                .toList();

        ofertasCompativeis.forEach(oferta -> {
            Matching matching = Matching.builder()
                    .oferta(oferta)
                    .demanda(demanda)
                    .status(StatusMatching.PENDENTE)
                    .build();
            matchingRepository.save(matching);
        });
    }

    @Transactional(readOnly = true)
    public List<MatchingDTO> listarPorOferta(Long ofertaId) {
        //  FIX: Verifica se o usuário logado é o dono da oferta
        Usuario usuario = securityUtils.getUsuarioLogado();
        OfertaFutura oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada: " + ofertaId));

        if (!oferta.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para ver os matches desta oferta");
        }

        return matchingRepository.findByOfertaId(ofertaId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchingDTO> listarPorDemanda(Long demandaId) {
        //  FIX: Verifica se o usuário logado é o dono da demanda
        Usuario usuario = securityUtils.getUsuarioLogado();
        Demanda demanda = demandaRepository.findById(demandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + demandaId));

        if (!demanda.getComprador().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para ver os matches desta demanda");
        }

        return matchingRepository.findByDemandaId(demandaId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    public MatchingDTO aceitar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Matching matching = matchingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matching não encontrado com id: " + id));

        if (matching.getStatus() != StatusMatching.PENDENTE) {
            throw new IllegalArgumentException("Apenas matchings pendentes podem ser aceitos");
        }

        //  FIX: Apenas o VENDEDOR dono da oferta pode aceitar o matching
        // Antes: qualquer pessoa (inclusive não autenticada) podia aceitar qualquer matching
        boolean isVendedorDono = matching.getOferta().getUsuario().getId().equals(usuario.getId());
        if (!isVendedorDono) {
            throw new IllegalArgumentException("Apenas o vendedor dono da oferta pode aceitar este matching");
        }

        matching.setStatus(StatusMatching.ACEITO);
        matching.getOferta().setStatus(StatusOferta.FECHADA);
        matching.getDemanda().setStatus(StatusDemanda.ATENDIDA);

        ofertaRepository.save(matching.getOferta());
        demandaRepository.save(matching.getDemanda());

        return toDTO(matchingRepository.save(matching));
    }

    @Transactional
    public MatchingDTO recusar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Matching matching = matchingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matching não encontrado com id: " + id));

        if (matching.getStatus() != StatusMatching.PENDENTE) {
            throw new IllegalArgumentException("Apenas matchings pendentes podem ser recusados");
        }

        //  FIX: Apenas o VENDEDOR dono da oferta OU o COMPRADOR dono da demanda pode recusar
        // Antes: qualquer pessoa (inclusive não autenticada) podia recusar qualquer matching
        boolean isVendedor = matching.getOferta().getUsuario().getId().equals(usuario.getId());
        boolean isComprador = matching.getDemanda().getComprador().getId().equals(usuario.getId());
        if (!isVendedor && !isComprador) {
            throw new IllegalArgumentException("Você não tem permissão para recusar este matching");
        }

        matching.setStatus(StatusMatching.RECUSADO);
        return toDTO(matchingRepository.save(matching));
    }

    private MatchingDTO toDTO(Matching m) {
        return MatchingDTO.builder()
                .id(m.getId())
                .ofertaId(m.getOferta().getId())
                .vendedorNome(m.getOferta().getUsuario().getNome())
                .produtoNome(m.getOferta().getProduto().getNome())
                .demandaId(m.getDemanda().getId())
                .compradorNome(m.getDemanda().getComprador().getNome())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .build();
    }
}