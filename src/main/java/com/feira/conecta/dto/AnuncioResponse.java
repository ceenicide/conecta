package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.feira.conecta.domain.StatusAnuncio;

/**
 * O que o frontend recebe — inclui dados enriquecidos gerados pelo backend.
 * Java Record: imutável, sem setters, não pode ser manipulado acidentalmente.
 */
public record AnuncioResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        Long produtoId,
        String produtoNome,
        BigDecimal quantidade,
        BigDecimal preco,
        StatusAnuncio status,
        LocalDateTime createdAt
) {}