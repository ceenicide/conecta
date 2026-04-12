package com.feira.conecta.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String erro,
    String mensagem,
    LocalDateTime timestamp
) {}