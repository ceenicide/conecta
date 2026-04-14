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
import com.feira.conecta.dto.AnuncioRequest;
import com.feira.conecta.dto.AnuncioResponse;
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
    private final AnuncioMatchingService anuncioMatchingService;

    @Transactional
    public AnuncioResponse criar(AnuncioRequest request) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        if (usuario.getTipo() != TipoUsuario.VENDEDOR) {
            throw new IllegalArgumentException("Apenas vendedores podem criar anúncios");
        }

        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto não encontrado com id: " + request.produtoId()));

        Anuncio anuncio = Anuncio.builder()
                .usuario(usuario)
                .produto(produto)
                .quantidade(request.quantidade())
                .preco(request.preco())
                .status(StatusAnuncio.ATIVO)
                .createdAt(LocalDateTime.now())
                .build();

        Anuncio salvo = anuncioRepository.save(anuncio);
        anuncioMatchingService.executarMatching(salvo);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponse> listarAtivos() {
        return anuncioRepository.findByStatus(StatusAnuncio.ATIVO).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorId(Long id) {
        return anuncioRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<AnuncioResponse> listarMeusAnuncios() {
        Usuario usuario = securityUtils.getUsuarioLogado();
        return anuncioRepository.findByUsuarioId(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnuncioResponse marcarComoVendido(Long id) {
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
        return toResponse(anuncioRepository.save(anuncio));
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

    private AnuncioResponse toResponse(Anuncio anuncio) {
        return new AnuncioResponse(
                anuncio.getId(),
                anuncio.getUsuario().getId(),
                anuncio.getUsuario().getNome(),
                anuncio.getProduto().getId(),
                anuncio.getProduto().getNome(),
                anuncio.getQuantidade(),
                anuncio.getPreco(),
                anuncio.getStatus(),
                anuncio.getCreatedAt()
        );
    }
}