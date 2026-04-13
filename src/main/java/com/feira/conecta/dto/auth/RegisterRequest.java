package com.feira.conecta.dto.auth;

import com.feira.conecta.domain.TipoUsuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para registro de novo usuário.
 *
 * Usando Java Record: imutável, sem boilerplate, perfeito para DTOs de entrada.
 * Cada campo tem sua própria validação — erros granulares chegam ao frontend.
 */
public record RegisterRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(
                regexp = "^\\(\\d{2}\\) \\d{9}$",
                message = "Telefone deve estar no formato (99) 999999999"
        )
        String telefone,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotNull(message = "Tipo é obrigatório")
        TipoUsuario tipo
) {}