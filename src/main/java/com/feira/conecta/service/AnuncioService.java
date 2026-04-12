package com.feira.conecta.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.AnuncioDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.AnuncioRepository;
import com.feira.conecta.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final ProdutoRepository produtoRepository;
    private final SecurityUtils securityUtils;
    // NOVO: injetado para disparar o matching assíncrono após salvar o anúncio
    private final AnuncioMatchingService anuncioMatchingService;

    @Transactional
    public AnuncioDTO criar(AnuncioDTO dto) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        if (usuario.getTipo() != TipoUsuario.VENDEDOR) {
            throw new IllegalArgumentException("Apenas vendedores podem criar anúncios");
        }

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto não encontrado com id: " + dto.getProdutoId()));

        Anuncio anuncio = Anuncio.builder()
                .usuario(usuario)
                .produto(produto)
                .quantidade(dto.getQuantidade())
                .preco(dto.getPreco())
                .status(StatusAnuncio.ATIVO)
                .createdAt(LocalDateTime.now())
                .build();

        Anuncio salvo = anuncioRepository.save(anuncio);

        // GATILHO: dispara o matching em background sem bloquear a resposta HTTP.
        // Como @Async roda em thread separada, o anúncio já foi comitado pelo
        // @Transactional desta camada antes do matching começar a consultar.
        anuncioMatchingService.executarMatching(salvo);

        return toDTO(salvo);
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
    public List<AnuncioDTO> listarMeusAnuncios() {
        Usuario usuario = securityUtils.getUsuarioLogado();
        return anuncioRepository.findByUsuarioId(usuario.getId()).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public AnuncioDTO marcarComoVendido(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Anuncio anuncio = anuncioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + id));

        if (!anuncio.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para alterar este anúncio");
        }

        if (anuncio.getStatus() == StatusAnuncio.VENDIDO) {
            throw new IllegalArgumentException("Anúncio já está marcado como vendido");
        }

        anuncio.setStatus(StatusAnuncio.VENDIDO);
        return toDTO(anuncioRepository.save(anuncio));
    }

    @Transactional
    public void deletar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Anuncio anuncio = anuncioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + id));

        if (!anuncio.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para deletar este anúncio");
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