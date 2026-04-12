package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.feira.conecta.domain.StatusAnuncio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnuncioDTO {

    private Long id;

    // Preenchido internamente pelo token — ignorado na entrada
    private Long usuarioId;
    private String usuarioNome;

    @NotNull(message = "Produto é obrigatório")
    private Long produtoId;
    private String produtoNome;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    private BigDecimal preco;

    private StatusAnuncio status;
    private LocalDateTime createdAt;
}
