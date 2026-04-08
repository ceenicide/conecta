package com.feira.conecta.dto;

import java.time.LocalDateTime;

import com.feira.conecta.domain.TipoUsuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UsuarioDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "Telefone é obrigatório")
    // FIX: mesmo padrão do AuthDTO — (99) 999999999
    @Pattern(
        regexp = "^\\(\\d{2}\\) \\d{9}$",
        message = "Telefone deve estar no formato (99) 999999999"
    )
    private String telefone;

    @NotNull(message = "Tipo é obrigatório")
    private TipoUsuario tipo;

    private LocalDateTime createdAt;
}