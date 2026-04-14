package com.feira.conecta.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * O que o frontend envia para criar/atualizar um anúncio.
 * Apenas campos que o cliente controla — sem id, status, datas ou dados do usuário.
 */
public record AnuncioRequest(

        @NotNull(message = "Produto é obrigatório")
        Long produtoId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidade,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal preco
) {}