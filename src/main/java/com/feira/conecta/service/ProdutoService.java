package com.feira.conecta.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.domain.Produto;
import com.feira.conecta.dto.ProdutoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository repository;

@Transactional
public ProdutoDTO criar(ProdutoDTO dto) {
        Produto produto = Produto.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .build();

        Produto salvo = repository.save(produto);

        return toDTO(salvo);
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
        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com id: " + id));

        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());

        return toDTO(repository.save(produto));
}

@Transactional
public void deletar(Long id) {
        if (!repository.existsById(id)) {
        throw new ResourceNotFoundException("Produto não encontrado com id: " + id);
        }
        repository.deleteById(id);
}

private ProdutoDTO toDTO(Produto produto) {
        return ProdutoDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .build();
}


}