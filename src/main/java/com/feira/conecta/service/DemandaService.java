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
import com.feira.conecta.dto.DemandaDTO;
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
    public DemandaDTO criar(DemandaDTO dto) {
        Usuario comprador = securityUtils.getUsuarioLogado();

        if (comprador.getTipo() != TipoUsuario.COMPRADOR) {
            throw new IllegalArgumentException("Apenas compradores podem criar demandas");
        }

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto não encontrado com id: " + dto.getProdutoId()));

        Demanda demanda = Demanda.builder()
                .comprador(comprador)
                .produto(produto)
                .quantidade(dto.getQuantidade())
                .dataLimite(dto.getDataLimite())
                .status(StatusDemanda.PROCURANDO)
                .build();

        Demanda salva = repository.save(demanda);
        matchingService.buscarMatchesPorDemanda(salva);

        return toDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<DemandaDTO> listarProcurando() {
        return repository.findByStatus(StatusDemanda.PROCURANDO).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandaDTO> listarMinhasDemandas() {
        Usuario comprador = securityUtils.getUsuarioLogado();
        return repository.findByCompradorId(comprador.getId()).stream()
                .map(this::toDTO).toList();
    }

    /**
     * NOVO: retorna todas as ofertas futuras ABERTAS cuja dataDisponivel
     * está dentro do prazo (dataLimite) da demanda informada.
     *
     * Exemplo: demanda com dataLimite = 13-05-2026 → retorna todas as
     * ofertas com dataDisponivel <= 13-05-2026 e status ABERTA.
     *
     * Regra de segurança: apenas o próprio comprador pode consultar
     * as ofertas compatíveis com sua demanda.
     */
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

    private DemandaDTO toDTO(Demanda d) {
        return DemandaDTO.builder()
                .id(d.getId())
                .compradorId(d.getComprador().getId())
                .compradorNome(d.getComprador().getNome())
                .produtoId(d.getProduto().getId())
                .produtoNome(d.getProduto().getNome())
                .quantidade(d.getQuantidade())
                .dataLimite(d.getDataLimite())
                .status(d.getStatus())
                .build();
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