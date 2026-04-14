package com.feira.conecta.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Demanda;
import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.DemandaRequest;
import com.feira.conecta.dto.DemandaResponse;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.DemandaRepository;
import com.feira.conecta.repository.OfertaFuturaRepository;
import com.feira.conecta.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DemandaService {

    private final DemandaRepository repository;
    private final ProdutoRepository produtoRepository;
    private final OfertaFuturaRepository ofertaFuturaRepository;
    private final MatchingService matchingService;
    private final SecurityUtils securityUtils;

    @Transactional
    public DemandaResponse criar(DemandaRequest request) {
        Usuario comprador = securityUtils.getUsuarioLogado();

        if (comprador.getTipo() != TipoUsuario.COMPRADOR) {
            throw new IllegalArgumentException("Apenas compradores podem criar demandas");
        }

        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto não encontrado com id: " + request.produtoId()));

        Demanda demanda = Demanda.builder()
                .comprador(comprador)
                .produto(produto)
                .quantidade(request.quantidade())
                .dataLimite(request.dataLimite())
                .status(StatusDemanda.PROCURANDO)
                .build();

        Demanda salva = repository.save(demanda);
        matchingService.buscarMatchesPorDemanda(salva);

        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<DemandaResponse> listarProcurando() {
        return repository.findByStatus(StatusDemanda.PROCURANDO).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandaResponse> listarMinhasDemandas() {
        Usuario comprador = securityUtils.getUsuarioLogado();
        return repository.findByCompradorId(comprador.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OfertaFuturaDTO> listarOfertasCompativeis(Long demandaId) {
        Usuario comprador = securityUtils.getUsuarioLogado();

        Demanda demanda = repository.findById(demandaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demanda não encontrada com id: " + demandaId));

        if (!demanda.getComprador().getId().equals(comprador.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para acessar esta demanda");
        }

        return ofertaFuturaRepository
                .findByStatusAndDataDisponivelLessThanEqual(StatusOferta.ABERTA, demanda.getDataLimite())
                .stream()
                .map(this::toOfertaDTO)
                .toList();
    }

    private DemandaResponse toResponse(Demanda d) {
        return new DemandaResponse(
                d.getId(),
                d.getComprador().getId(),
                d.getComprador().getNome(),
                d.getProduto().getId(),
                d.getProduto().getNome(),
                d.getQuantidade(),
                d.getDataLimite(),
                d.getStatus()
        );
    }

    private OfertaFuturaDTO toOfertaDTO(OfertaFutura o) {
        return OfertaFuturaDTO.builder()
                .id(o.getId())
                .usuarioId(o.getUsuario().getId())
                .usuarioNome(o.getUsuario().getNome())
                .produtoId(o.getProduto().getId())
                .produtoNome(o.getProduto().getNome())
                .quantidade(o.getQuantidade())
                .dataDisponivel(o.getDataDisponivel())
                .status(o.getStatus())
                .build();
    }
}