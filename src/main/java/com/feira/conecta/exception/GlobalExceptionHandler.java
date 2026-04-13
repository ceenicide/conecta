package com.feira.conecta.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Handler global de exceções — retorna JSON limpo ao frontend.
 *
 * Cada handler captura um tipo específico de problema e devolve:
 *   - status HTTP correto
 *   - mensagem legível (sem stack trace)
 *   - timestamp para correlação de logs
 *
 * A estrutura ErrorResponse é um Java Record imutável — sem getters manuais.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domínio ────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErrorResponse(404, "Recurso não encontrado", ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(400, "Requisição inválida", ex.getMessage(), LocalDateTime.now())
        );
    }

    // ── Validação (@Valid) ──────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(400, "Erro de validação", mensagem, LocalDateTime.now())
        );
    }

    // ── HTTP / Infraestrutura ──────────────────────────────────────────────

    /**
     * JSON mal formado no body (ex: vírgula faltando, campo com tipo errado).
     * Antes chegava como 500 — agora é 400 com mensagem clara.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(400, "Corpo da requisição inválido",
                "Verifique se o JSON está bem formado e os tipos dos campos estão corretos",
                LocalDateTime.now())
        );
    }

    /**
     * Parâmetro de URL com tipo errado (ex: /anuncios/abc quando espera Long).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Parâmetro '" + ex.getName() + "' inválido: esperado "
                + ex.getRequiredType().getSimpleName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(400, "Parâmetro inválido", mensagem, LocalDateTime.now())
        );
    }

    /**
     * Parâmetro obrigatório ausente na query string.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(400, "Parâmetro ausente",
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório",
                LocalDateTime.now())
        );
    }

    /**
     * Método HTTP não suportado (ex: POST em endpoint que aceita só GET).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            new ErrorResponse(405, "Método não permitido",
                ex.getMethod() + " não é suportado neste endpoint",
                LocalDateTime.now())
        );
    }

    /**
     * Acesso negado pelo Spring Security (403).
     * Separado do 401 para o frontend saber distinguir:
     *   401 = não autenticado (sem token / token expirado)
     *   403 = autenticado mas sem permissão
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ErrorResponse(403, "Acesso negado",
                "Você não tem permissão para acessar este recurso",
                LocalDateTime.now())
        );
    }

    // ── Fallback ────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErrorResponse(500, "Erro interno", "Ocorreu um erro inesperado", LocalDateTime.now())
        );
    }
}