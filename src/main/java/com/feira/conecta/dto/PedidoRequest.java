package com.feira.conecta.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * O que o frontend envia para criar um pedido.
 * O comprador vem do token JWT — nunca do body.
 */
public record PedidoRequest(

        @NotNull(message = "Anúncio é obrigatório")
        Long anuncioId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidade
) {}