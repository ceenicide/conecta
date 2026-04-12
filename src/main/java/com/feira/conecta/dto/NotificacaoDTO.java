package com.feira.conecta.dto;

import java.time.LocalDateTime;

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
public class NotificacaoDTO {

    private Long id;
    private Long usuarioId;
    private String mensagem;
    private boolean lida;
    private LocalDateTime dataCriacao;
}