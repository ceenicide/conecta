package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.feira.conecta.domain.StatusDemanda;

/**
 * O que o frontend recebe ao consultar uma demanda.
 */
public record DemandaResponse(
        Long id,
        Long compradorId,
        String compradorNome,
        Long produtoId,
        String produtoNome,
        BigDecimal quantidade,
        LocalDate dataLimite,
        StatusDemanda status
) {}