package com.feira.conecta.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.domain.StatusPedido;
import com.feira.conecta.dto.PedidoRequest;
import com.feira.conecta.dto.PedidoResponse;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.PedidoService;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock private PedidoService service;

    @InjectMocks
    private PedidoController controller;

    private PedidoRequest request;
    private PedidoResponse response;

    @BeforeEach
    void setup() {
        request = new PedidoRequest(1L, new BigDecimal("10"));

        response = new PedidoResponse(
                1L, 2L, "Carlos", 1L, "Soja", "Maria",
                new BigDecimal("10"), StatusPedido.PENDENTE, null
        );
    }

    @Test
    void deveCriarPedidoERetornar201() {
        when(service.criar(any())).thenReturn(response);

        ResponseEntity<PedidoResponse> resposta = controller.criar(request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(resposta.getBody().status()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(resposta.getBody().compradorNome()).isEqualTo("Carlos");
        assertThat(resposta.getBody().vendedorNome()).isEqualTo("Maria");
    }

    @Test
    void deveBuscarPedidoPorIdERetornar200() {
        when(service.buscarPorId(1L)).thenReturn(response);

        ResponseEntity<PedidoResponse> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().id()).isEqualTo(1L);
    }

    @Test
    void deveListarMeusPedidosERetornar200() {
        when(service.listarMeusPedidos()).thenReturn(List.of(response));

        ResponseEntity<List<PedidoResponse>> resposta = controller.listarMeusPedidos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveConfirmarPedidoERetornar200() {
        PedidoResponse confirmado = new PedidoResponse(
                1L, 2L, "Carlos", 1L, "Soja", "Maria",
                new BigDecimal("10"), StatusPedido.CONFIRMADO, null
        );
        when(service.confirmar(1L)).thenReturn(confirmado);

        ResponseEntity<PedidoResponse> resposta = controller.confirmar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().status()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test
    void deveFinalizarPedidoERetornar200() {
        PedidoResponse finalizado = new PedidoResponse(
                1L, 2L, "Carlos", 1L, "Soja", "Maria",
                new BigDecimal("10"), StatusPedido.FINALIZADO, null
        );
        when(service.finalizar(1L)).thenReturn(finalizado);

        ResponseEntity<PedidoResponse> resposta = controller.finalizar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().status()).isEqualTo(StatusPedido.FINALIZADO);
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Pedido não encontrado com id: 99"));

        assertThatThrownBy(() -> controller.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Pedido não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoSemPermissaoParaConfirmar() {
        when(service.confirmar(1L))
                .thenThrow(new IllegalArgumentException("Você não tem permissão para confirmar este pedido"));

        assertThatThrownBy(() -> controller.confirmar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para confirmar este pedido");
    }
}