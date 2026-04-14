package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.feira.conecta.domain.StatusPedido;

/**
 * O que o frontend recebe — inclui nomes enriquecidos para exibição.
 */
public record PedidoResponse(
        Long id,
        Long compradorId,
        String compradorNome,
        Long anuncioId,
        String produtoNome,
        String vendedorNome,
        BigDecimal quantidade,
        StatusPedido status,
        LocalDateTime createdAt
) {}