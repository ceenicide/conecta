package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

/**
 * O que o frontend envia para criar uma demanda.
 * O comprador vem do token JWT — nunca do body.
 */
public record DemandaRequest(

        @NotNull(message = "Produto é obrigatório")
        Long produtoId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidade,

        @NotNull(message = "Data limite é obrigatória")
        @Future(message = "Data deve ser futura")
        LocalDate dataLimite
) {}