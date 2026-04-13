package com.feira.conecta.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de entrada para login.
 *
 * Separado do RegisterRequest porque login não precisa de nome/tipo —
 * um DTO único com campos opcionais seria ambíguo e mal documentado no Swagger.
 */
public record LoginRequest(

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(
                regexp = "^\\(\\d{2}\\) \\d{9}$",
                message = "Telefone deve estar no formato (99) 999999999"
        )
        String telefone,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {}