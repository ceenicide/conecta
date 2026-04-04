package com.feira.conecta.service;

import com.feira.conecta.domain.*;
import com.feira.conecta.dto.DemandaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandaService {

    private final DemandaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final MatchingService matchingService;

    @Transactional
    public DemandaDTO criar(DemandaDTO dto) {
        Usuario comprador = usuarioRepository.findById(dto.getCompradorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com id: " + dto.getCompradorId()));

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

        // busca matches automaticamente após criar
        matchingService.buscarMatchesPorDemanda(salva);

        return toDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<DemandaDTO> listarProcurando() {
        return repository.findByStatus(StatusDemanda.PROCURANDO).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<DemandaDTO> listarPorComprador(Long compradorId) {
        return repository.findByCompradorId(compradorId).stream()
                .map(this::toDTO).toList();
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
}