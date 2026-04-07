package com.feira.conecta.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.ProdutoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

private final ProdutoRepository repository;
    private final SecurityUtils securityUtils; //       FIX: Injetado para verificar ownership

@Transactional
public ProdutoDTO criar(ProdutoDTO dto) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        // FIX: Apenas VENDEDOR pode cadastrar produtos no catálogo
        if (usuario.getTipo() != TipoUsuario.VENDEDOR) {
        throw new IllegalArgumentException("Apenas vendedores podem cadastrar produtos");
        }

        Produto produto = Produto.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .usuario(usuario) //  FIX: Vincula o produto ao usuário logado (nunca do body)
                .build();

        return toDTO(repository.save(produto));
}

@Transactional(readOnly = true)
public List<ProdutoDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
}

@Transactional(readOnly = true)
public ProdutoDTO buscarPorId(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));
}

@Transactional
public ProdutoDTO atualizar(Long id, ProdutoDTO dto) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));

        //  FIX: Apenas o dono pode editar o produto — impede IDOR/BOLA
        if (!produto.getUsuario().getId().equals(usuario.getId())) {
        throw new IllegalArgumentException("Você não tem permissão para editar este produto");
        }

        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());

        return toDTO(repository.save(produto));
}

@Transactional
public void deletar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));

        //  FIX: Apenas o dono pode deletar o produto — impede IDOR/BOLA
        if (!produto.getUsuario().getId().equals(usuario.getId())) {
        throw new IllegalArgumentException("Você não tem permissão para deletar este produto");
        }

        repository.deleteById(id);
}

private ProdutoDTO toDTO(Produto produto) {
        return ProdutoDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .usuarioId(produto.getUsuario().getId())         // ✅ FIX: expõe dono na resposta
                .usuarioNome(produto.getUsuario().getNome())
                .build();
}
}