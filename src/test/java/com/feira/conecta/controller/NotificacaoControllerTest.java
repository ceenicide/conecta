package com.feira.conecta.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.dto.NotificacaoDTO;
import com.feira.conecta.service.NotificacaoService;

@ExtendWith(MockitoExtension.class)
class NotificacaoControllerTest {

    @Mock private NotificacaoService service;

    @InjectMocks
    private NotificacaoController controller;

    private NotificacaoDTO dto;

    @BeforeEach
    void setup() {
        dto = NotificacaoDTO.builder()
                .id(1L)
                .usuarioId(1L)
                .mensagem("🎯 Match encontrado para Soja!")
                .lida(false)
                .dataCriacao(LocalDateTime.now())
                .build();
    }

    @Test
    void deveListarNotificacoesNaoLidasERetornar200() {
        when(service.listarNaoLidas()).thenReturn(List.of(dto));

        ResponseEntity<List<NotificacaoDTO>> resposta = controller.listarNaoLidas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).isLida()).isFalse();
    }

    @Test
    void deveListarTodasNotificacoesERetornar200() {
        NotificacaoDTO lida = NotificacaoDTO.builder()
                .id(2L).usuarioId(1L).mensagem("Anterior").lida(true)
                .dataCriacao(LocalDateTime.now().minusDays(1)).build();

        when(service.listarTodas()).thenReturn(List.of(dto, lida));

        ResponseEntity<List<NotificacaoDTO>> resposta = controller.listarTodas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(2);
    }

    @Test
    void deveRetornarContagemDeNaoLidasERetornar200() {
        when(service.contarNaoLidas()).thenReturn(7L);

        ResponseEntity<Map<String, Long>> resposta = controller.contarNaoLidas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).containsEntry("naoLidas", 7L);
    }

    @Test
    void deveMarcarNotificacaoComoLidaERetornar200() {
        NotificacaoDTO lida = NotificacaoDTO.builder()
                .id(1L).usuarioId(1L).mensagem("msg").lida(true)
                .dataCriacao(LocalDateTime.now()).build();

        when(service.marcarComoLida(1L)).thenReturn(lida);

        ResponseEntity<NotificacaoDTO> resposta = controller.marcarComoLida(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().isLida()).isTrue();
        verify(service, times(1)).marcarComoLida(1L);
    }

    @Test
    void deveMarcarTodasComoLidasERetornar200() {
        when(service.marcarTodasComoLidas()).thenReturn(4);

        ResponseEntity<Map<String, Integer>> resposta = controller.marcarTodasComoLidas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).containsEntry("notificacoesAtualizadas", 4);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverNotificacoes() {
        when(service.listarNaoLidas()).thenReturn(List.of());

        ResponseEntity<List<NotificacaoDTO>> resposta = controller.listarNaoLidas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isEmpty();
    }
}