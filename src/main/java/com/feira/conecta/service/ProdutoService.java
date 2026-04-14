package com.feira.conecta.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.ProdutoRequest;
import com.feira.conecta.dto.ProdutoResponse;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository repository;
    private final SecurityUtils securityUtils;

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        if (usuario.getTipo() != TipoUsuario.VENDEDOR) {
            throw new IllegalArgumentException("Apenas vendedores podem cadastrar produtos");
        }

        Produto produto = Produto.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .usuario(usuario)
                .build();

        return toResponse(repository.save(produto));
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarTodos() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));

        if (!produto.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para editar este produto");
        }

        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());

        return toResponse(repository.save(produto));
    }

    @Transactional
    public void deletar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));

        if (!produto.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para deletar este produto");
        }

        repository.deleteById(id);
    }

    private ProdutoResponse toResponse(Produto produto) {
        Long usuarioId = produto.getUsuario() != null ? produto.getUsuario().getId() : null;
        String usuarioNome = produto.getUsuario() != null ? produto.getUsuario().getNome() : null;

        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                usuarioId,
                usuarioNome
        );
    }
}