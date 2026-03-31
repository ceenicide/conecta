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
import com.feira.conecta.dto.PedidoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.PedidoService;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService service;

    @InjectMocks
    private PedidoController controller;

    private PedidoDTO dto;

    @BeforeEach
    void setup() {
        dto = PedidoDTO.builder()
                .id(1L)
                .compradorId(2L)
                .compradorNome("Carlos")
                .anuncioId(1L)
                .produtoNome("Soja")
                .vendedorNome("Maria")
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.PENDENTE)
                .build();
    }

    // ========================
    // CENÁRIOS FELIZES
    // ========================

    @Test
    void deveCriarPedidoERetornar200() {
        when(service.criar(any())).thenReturn(dto);

        ResponseEntity<PedidoDTO> resposta = controller.criar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(resposta.getBody().getCompradorNome()).isEqualTo("Carlos");
    }

    @Test
    void deveBuscarPedidoPorIdERetornar200() {
        when(service.buscarPorId(1L)).thenReturn(dto);

        ResponseEntity<PedidoDTO> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getProdutoNome()).isEqualTo("Soja");
    }

    @Test
    void deveListarPedidosPorCompradorERetornar200() {
        when(service.listarPorComprador(2L)).thenReturn(List.of(dto));

        ResponseEntity<List<PedidoDTO>> resposta = controller.listarPorComprador(2L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).getCompradorId()).isEqualTo(2L);
    }

    @Test
    void deveConfirmarPedidoERetornar200() {
        PedidoDTO confirmado = PedidoDTO.builder()
                .id(1L).compradorId(2L).anuncioId(1L)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.CONFIRMADO).build();

        when(service.confirmar(1L)).thenReturn(confirmado);

        ResponseEntity<PedidoDTO> resposta = controller.confirmar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test
    void deveFinalizarPedidoERetornar200() {
        PedidoDTO finalizado = PedidoDTO.builder()
                .id(1L).compradorId(2L).anuncioId(1L)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.FINALIZADO).build();

        when(service.finalizar(1L)).thenReturn(finalizado);

        ResponseEntity<PedidoDTO> resposta = controller.finalizar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusPedido.FINALIZADO);
    }

    @Test
    void deveRetornarListaVaziaQuandoCompradorNaoTiverPedidos() {
        when(service.listarPorComprador(2L)).thenReturn(List.of());

        ResponseEntity<List<PedidoDTO>> resposta = controller.listarPorComprador(2L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isEmpty();
    }

    // ========================
    // CENÁRIOS INFELIZES
    // ========================

    @Test
    void deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Pedido não encontrado com id: 99"));

        assertThatThrownBy(() -> controller.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Pedido não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoVendedorTentaFazerPedido() {
        when(service.criar(any()))
                .thenThrow(new IllegalArgumentException("Apenas compradores podem fazer pedidos"));

        assertThatThrownBy(() -> controller.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas compradores podem fazer pedidos");
    }

    @Test
    void deveLancarExcecaoAoConfirmarPedidoNaoPendente() {
        when(service.confirmar(1L))
                .thenThrow(new IllegalArgumentException("Apenas pedidos pendentes podem ser confirmados"));

        assertThatThrownBy(() -> controller.confirmar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas pedidos pendentes podem ser confirmados");
    }

    @Test
    void deveLancarExcecaoAoFinalizarPedidoNaoConfirmado() {
        when(service.finalizar(1L))
                .thenThrow(new IllegalArgumentException("Apenas pedidos confirmados podem ser finalizados"));

        assertThatThrownBy(() -> controller.finalizar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas pedidos confirmados podem ser finalizados");
    }
    @Test
void deveLancarExcecaoAoListarPedidosDeCompradorInexistente() {
    when(service.listarPorComprador(99L))
            .thenThrow(new ResourceNotFoundException("Usuário não encontrado com id: 99"));

    assertThatThrownBy(() -> controller.listarPorComprador(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Usuário não encontrado com id: 99");
}
}