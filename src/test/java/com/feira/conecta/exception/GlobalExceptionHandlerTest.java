package com.feira.conecta.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveRetornar404ParaResourceNotFoundException() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Produto não encontrado com id: 1");

        ResponseEntity<ErrorResponse> resposta = handler.handleNotFound(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(404);
        assertThat(resposta.getBody().status()).isEqualTo(404);
        assertThat(resposta.getBody().erro()).isEqualTo("Recurso não encontrado");
        assertThat(resposta.getBody().mensagem()).isEqualTo("Produto não encontrado com id: 1");
    }

    @Test
    void deveRetornar400ParaIllegalArgumentException() {
        IllegalArgumentException ex =
                new IllegalArgumentException("Apenas vendedores podem criar anúncios");

        ResponseEntity<ErrorResponse> resposta = handler.handleBadRequest(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody().status()).isEqualTo(400);
        assertThat(resposta.getBody().erro()).isEqualTo("Requisição inválida");
        assertThat(resposta.getBody().mensagem()).isEqualTo("Apenas vendedores podem criar anúncios");
    }

    @Test
    void deveRetornar500ParaExcecaoGenerica() {
        Exception ex = new Exception("Erro inesperado");

        ResponseEntity<ErrorResponse> resposta = handler.handleGeneric(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getBody().status()).isEqualTo(500);
        assertThat(resposta.getBody().erro()).isEqualTo("Erro interno");
    }

    @Test
    void devePreencherTimestampNasRespostas() {
        ResourceNotFoundException ex = new ResourceNotFoundException("teste");

        ResponseEntity<ErrorResponse> resposta = handler.handleNotFound(ex);

        assertThat(resposta.getBody().timestamp()).isNotNull();
    }
}