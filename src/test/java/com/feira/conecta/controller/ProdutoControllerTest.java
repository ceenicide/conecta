package com.feira.conecta.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.dto.ProdutoRequest;
import com.feira.conecta.dto.ProdutoResponse;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.ProdutoService;

@ExtendWith(MockitoExtension.class)
class ProdutoControllerTest {

    @Mock private ProdutoService service;

    @InjectMocks
    private ProdutoController controller;

    private ProdutoRequest request;
    private ProdutoResponse response;

    @BeforeEach
    void setup() {
        request = new ProdutoRequest("Soja", "Safra 2025");
        response = new ProdutoResponse(1L, "Soja", "Safra 2025", 1L, "Maria");
    }

    @Test
    void deveCriarProdutoERetornar201() {
        when(service.criar(any())).thenReturn(response);

        ResponseEntity<ProdutoResponse> resposta = controller.criar(request);

        // POST de criação deve retornar 201 Created
        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(resposta.getBody().nome()).isEqualTo("Soja");
    }

    @Test
    void deveListarProdutosERetornar200() {
        when(service.listarTodos()).thenReturn(List.of(response));

        ResponseEntity<List<ProdutoResponse>> resposta = controller.listarTodos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).nome()).isEqualTo("Soja");
    }

    @Test
    void deveBuscarProdutoPorIdERetornar200() {
        when(service.buscarPorId(1L)).thenReturn(response);

        ResponseEntity<ProdutoResponse> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().nome()).isEqualTo("Soja");
    }

    @Test
    void deveAtualizarProdutoERetornar200() {
        ProdutoResponse atualizado = new ProdutoResponse(1L, "Soja Premium", "Safra 2026", 1L, "Maria");
        when(service.atualizar(eq(1L), any())).thenReturn(atualizado);

        ResponseEntity<ProdutoResponse> resposta = controller.atualizar(1L, request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().nome()).isEqualTo("Soja Premium");
    }

    @Test
    void deveDeletarProdutoERetornar204() {
        doNothing().when(service).deletar(1L);

        ResponseEntity<Void> resposta = controller.deletar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(204);
        verify(service, times(1)).deletar(1L);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverProdutos() {
        when(service.listarTodos()).thenReturn(List.of());

        ResponseEntity<List<ProdutoResponse>> resposta = controller.listarTodos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoEncontrado() {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Produto não encontrado com id: 99"));

        assertThatThrownBy(() -> controller.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoAoDeletarProdutoInexistente() {
        doThrow(new ResourceNotFoundException("Produto não encontrado com id: 99"))
                .when(service).deletar(99L);

        assertThatThrownBy(() -> controller.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 99");
    }
}