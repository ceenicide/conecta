package com.feira.conecta.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.AnuncioDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.AnuncioRepository;
import com.feira.conecta.repository.ProdutoRepository;
import com.feira.conecta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public AnuncioDTO criar(AnuncioDTO dto) {
        // busca o usuário — lança exceção se não existir
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com id: " + dto.getUsuarioId()));

        // regra de negócio: só VENDEDOR pode criar anúncio
        if (usuario.getTipo() != TipoUsuario.VENDEDOR) {
            throw new IllegalArgumentException("Apenas vendedores podem criar anúncios");
        }

        // busca o produto — lança exceção se não existir
        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto não encontrado com id: " + dto.getProdutoId()));

                        Anuncio anuncio = Anuncio.builder()
                        .usuario(usuario)
                        .produto(produto)
                        .quantidade(dto.getQuantidade())
                        .preco(dto.getPreco())
                        .status(StatusAnuncio.ATIVO)
                        .createdAt(LocalDateTime.now()) // ← adiciona isso
                        .build();

        return toDTO(anuncioRepository.save(anuncio));
    }

    @Transactional(readOnly = true)
    public List<AnuncioDTO> listarAtivos() {
        return anuncioRepository.findByStatus(StatusAnuncio.ATIVO).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnuncioDTO buscarPorId(Long id) {
        return anuncioRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<AnuncioDTO> listarPorUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException(
                    "Usuário não encontrado com id: " + usuarioId);
        }
        return anuncioRepository.findByUsuarioId(usuarioId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public AnuncioDTO marcarComoVendido(Long id) {
        Anuncio anuncio = anuncioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + id));

        // regra: não pode marcar como vendido o que já está vendido
        if (anuncio.getStatus() == StatusAnuncio.VENDIDO) {
            throw new IllegalArgumentException("Anúncio já está marcado como vendido");
        }

        anuncio.setStatus(StatusAnuncio.VENDIDO);
        return toDTO(anuncioRepository.save(anuncio));
    }

    @Transactional
    public void deletar(Long id) {
        if (!anuncioRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Anúncio não encontrado com id: " + id);
        }
        anuncioRepository.deleteById(id);
    }

    private AnuncioDTO toDTO(Anuncio anuncio) {
        return AnuncioDTO.builder()
                .id(anuncio.getId())
                .usuarioId(anuncio.getUsuario().getId())
                .usuarioNome(anuncio.getUsuario().getNome())
                .produtoId(anuncio.getProduto().getId())
                .produtoNome(anuncio.getProduto().getNome())
                .quantidade(anuncio.getQuantidade())
                .preco(anuncio.getPreco())
                .status(anuncio.getStatus())
                .createdAt(anuncio.getCreatedAt())
                .build();
    }
}