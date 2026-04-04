package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.feira.conecta.domain.StatusDemanda;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemandaDTO {
    private Long id;

    @NotNull(message = "Comprador é obrigatório")
    private Long compradorId;
    private String compradorNome;

    @NotNull(message = "Produto é obrigatório")
    private Long produtoId;
    private String produtoNome;

    @NotNull @DecimalMin("0.01")
    private BigDecimal quantidade;

    @NotNull(message = "Data limite é obrigatória")
    @Future(message = "Data deve ser futura")
    private LocalDate dataLimite;

    private StatusDemanda status;
}