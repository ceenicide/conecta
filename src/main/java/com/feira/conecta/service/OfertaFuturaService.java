package com.feira.conecta.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.OfertaFuturaRepository;
import com.feira.conecta.repository.ProdutoRepository;
import com.feira.conecta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OfertaFuturaService {

    private final OfertaFuturaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final MatchingService matchingService;

    @Transactional
    public OfertaFuturaDTO criar(OfertaFuturaDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com id: " + dto.getUsuarioId()));

        if (usuario.getTipo() != TipoUsuario.VENDEDOR) {
            throw new IllegalArgumentException("Apenas vendedores podem criar ofertas futuras");
        }

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto não encontrado com id: " + dto.getProdutoId()));

        OfertaFutura oferta = OfertaFutura.builder()
                .usuario(usuario)
                .produto(produto)
                .quantidade(dto.getQuantidade())
                .dataDisponivel(dto.getDataDisponivel())
                .status(StatusOferta.ABERTA)
                .build();

        OfertaFutura salva = repository.save(oferta);

        // busca matches automaticamente após criar
        matchingService.buscarMatchesPorOferta(salva);

        return toDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<OfertaFuturaDTO> listarAbertas() {
        return repository.findByStatus(StatusOferta.ABERTA).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<OfertaFuturaDTO> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId).stream()
                .map(this::toDTO).toList();
    }

    private OfertaFuturaDTO toDTO(OfertaFutura o) {
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