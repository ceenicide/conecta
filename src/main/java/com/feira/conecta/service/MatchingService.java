package com.feira.conecta.service;

import com.feira.conecta.domain.*;
import com.feira.conecta.dto.MatchingDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRepository matchingRepository;
    private final OfertaFuturaRepository ofertaRepository;
    private final DemandaRepository demandaRepository;

    @Transactional
    public void buscarMatchesPorOferta(OfertaFutura oferta) {
        List<Demanda> demandasCompativeis = demandaRepository
                .findByProdutoAndStatus(oferta.getProduto(), StatusDemanda.PROCURANDO)
                .stream()
                .filter(d -> !oferta.getDataDisponivel().isAfter(d.getDataLimite()))
                .filter(d -> oferta.getQuantidade().compareTo(d.getQuantidade()) >= 0)
                .filter(d -> !matchingRepository.existsByOfertaIdAndDemandaId(
                        oferta.getId(), d.getId()))
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
                .filter(o -> !matchingRepository.existsByOfertaIdAndDemandaId(
                        o.getId(), demanda.getId()))
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
        return matchingRepository.findByOfertaId(ofertaId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchingDTO> listarPorDemanda(Long demandaId) {
        return matchingRepository.findByDemandaId(demandaId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional
    public MatchingDTO aceitar(Long id) {
        Matching matching = matchingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matching não encontrado com id: " + id));

        if (matching.getStatus() != StatusMatching.PENDENTE) {
            throw new IllegalArgumentException("Apenas matchings pendentes podem ser aceitos");
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
        Matching matching = matchingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matching não encontrado com id: " + id));

        if (matching.getStatus() != StatusMatching.PENDENTE) {
            throw new IllegalArgumentException("Apenas matchings pendentes podem ser recusados");
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