package com.feira.conecta.dto;

/**
 * O que o frontend recebe ao consultar um produto.
 */
public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        Long usuarioId,
        String usuarioNome
) {}