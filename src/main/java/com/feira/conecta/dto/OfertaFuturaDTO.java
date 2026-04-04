package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.feira.conecta.domain.StatusOferta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfertaFuturaDTO {
    private Long id;

    @NotNull(message = "Usuário é obrigatório")
    private Long usuarioId;
    private String usuarioNome;

    @NotNull(message = "Produto é obrigatório")
    private Long produtoId;
    private String produtoNome;

    @NotNull @DecimalMin("0.01")
    private BigDecimal quantidade;

    @NotNull(message = "Data disponível é obrigatória")
    @Future(message = "Data deve ser futura")
    private LocalDate dataDisponivel;

    private StatusOferta status;
}