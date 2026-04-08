package com.feira.conecta.dto;

import com.feira.conecta.domain.TipoUsuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthDTO {

    @NotBlank(message = "Telefone é obrigatório")
    // FIX: formato brasileiro (99) 999999999 — 9 dígitos no número (celular com 9)
    // Aceita exatamente: (XX) XXXXXXXXX
    @Pattern(
        regexp = "^\\(\\d{2}\\) \\d{9}$",
        message = "Telefone deve estar no formato (99) 999999999"
    )
    private String telefone;

    private String nome;

    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    private TipoUsuario tipo;

    private String token;
}