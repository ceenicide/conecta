package com.feira.conecta.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.feira.conecta.dto.ProdutoDTO;
import com.feira.conecta.service.ProdutoService;

@ExtendWith(MockitoExtension.class)
class ProdutoControllerTest {

    @Mock
    private ProdutoService service;

    @InjectMocks
    private ProdutoController controller;

    // ========================
    // CENÁRIOS FELIZES
    // ========================

    @Test
    void deveCriarProdutoERetornar200() {
        ProdutoDTO dto = ProdutoDTO.builder()
                .id(1L).nome("Soja").descricao("Safra 2025").build();

        when(service.criar(any())).thenReturn(dto);

        ResponseEntity<ProdutoDTO> resposta = controller.criar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getNome()).isEqualTo("Soja");
    }

    @Test
    void deveListarProdutosERetornar200() {
        ProdutoDTO dto = ProdutoDTO.builder()
                .id(1L).nome("Milho").descricao("Milho verde").build();

        when(service.listarTodos()).thenReturn(List.of(dto));

        ResponseEntity<List<ProdutoDTO>> resposta = controller.listarTodos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).getNome()).isEqualTo("Milho");
    }

    @Test
    void deveBuscarProdutoPorIdERetornar200() {
        ProdutoDTO dto = ProdutoDTO.builder()
                .id(1L).nome("Arroz").descricao("Tipo 1").build();

        when(service.buscarPorId(1L)).thenReturn(dto);

        ResponseEntity<ProdutoDTO> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getNome()).isEqualTo("Arroz");
    }

    @Test
    void deveDeletarProdutoERetornar204() {
        doNothing().when(service).deletar(1L);

        ResponseEntity<Void> resposta = controller.deletar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(204);
        verify(service, times(1)).deletar(1L);
    }

    // ========================
    // CENÁRIOS INFELIZES
    // ========================

    @Test
    void deveLancarExcecaoQuandoProdutoNaoEncontrado() {
        when(service.buscarPorId(99L))
                .thenThrow(new RuntimeException("Produto não encontrado"));

        assertThatThrownBy(() -> controller.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado");
    }

    @Test
void deveRetornarListaVaziaQuandoNaoHouverProdutos() {
    when(service.listarTodos()).thenReturn(List.of());

    ResponseEntity<List<ProdutoDTO>> resposta = controller.listarTodos();

    assertThat(resposta.getStatusCode().value()).isEqualTo(200);
    assertThat(resposta.getBody()).isEmpty();
}

@Test
void deveLancarExcecaoAoDeletarProdutoInexistente() {
    doThrow(new RuntimeException("Produto não encontrado"))
            .when(service).deletar(99L);

    assertThatThrownBy(() -> controller.deletar(99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Produto não encontrado");
}

@Test
void deveAtualizarProdutoERetornar200() {
    ProdutoDTO dto = ProdutoDTO.builder()
            .id(1L).nome("Soja Premium").descricao("Safra 2026").build();

    when(service.atualizar(eq(1L), any())).thenReturn(dto);

    ResponseEntity<ProdutoDTO> resposta = controller.atualizar(1L, dto);

    assertThat(resposta.getStatusCode().value()).isEqualTo(200);
    assertThat(resposta.getBody().getNome()).isEqualTo("Soja Premium");
}
}