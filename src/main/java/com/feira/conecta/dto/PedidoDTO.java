package com.feira.conecta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.feira.conecta.domain.StatusPedido;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PedidoDTO {

    private Long id;

    // Preenchido internamente pelo token — ignorado na entrada
    private Long compradorId;
    private String compradorNome;
    private String produtoNome;
    private String vendedorNome;

    @NotNull(message = "Anúncio é obrigatório")
    private Long anuncioId;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    private StatusPedido status;
    private LocalDateTime createdAt;
}
