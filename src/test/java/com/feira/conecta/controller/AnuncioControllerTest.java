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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.dto.AnuncioDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.AnuncioService;

@ExtendWith(MockitoExtension.class)
class AnuncioControllerTest {

    @Mock
    private AnuncioService service;

    @InjectMocks
    private AnuncioController controller;

    private AnuncioDTO dto;

    @BeforeEach
    void setup() {
        dto = AnuncioDTO.builder()
                .id(1L)
                .usuarioId(1L)
                .usuarioNome("Maria")
                .produtoId(1L)
                .produtoNome("Soja")
                .quantidade(new BigDecimal("100"))
                .preco(new BigDecimal("50.00"))
                .status(StatusAnuncio.ATIVO)
                .build();
    }

    // ========================
    // CENÁRIOS FELIZES
    // ========================

    @Test
    void deveCriarAnuncioERetornar200() {
        when(service.criar(any())).thenReturn(dto);

        ResponseEntity<AnuncioDTO> resposta = controller.criar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(resposta.getBody().getUsuarioNome()).isEqualTo("Maria");
    }

    @Test
    void deveListarAnunciosAtivosERetornar200() {
        when(service.listarAtivos()).thenReturn(List.of(dto));

        ResponseEntity<List<AnuncioDTO>> resposta = controller.listarAtivos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void deveBuscarAnuncioPorIdERetornar200() {
        when(service.buscarPorId(1L)).thenReturn(dto);

        ResponseEntity<AnuncioDTO> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getProdutoNome()).isEqualTo("Soja");
    }

    @Test
    void deveListarAnunciosPorUsuarioERetornar200() {
        when(service.listarPorUsuario(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<AnuncioDTO>> resposta = controller.listarPorUsuario(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().get(0).getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void deveMarcarAnuncioComoVendidoERetornar200() {
        AnuncioDTO vendido = AnuncioDTO.builder()
                .id(1L).usuarioId(1L).produtoId(1L)
                .quantidade(new BigDecimal("100"))
                .preco(new BigDecimal("50.00"))
                .status(StatusAnuncio.VENDIDO)
                .build();

        when(service.marcarComoVendido(1L)).thenReturn(vendido);

        ResponseEntity<AnuncioDTO> resposta = controller.marcarComoVendido(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusAnuncio.VENDIDO);
    }

    @Test
    void deveDeletarAnuncioERetornar204() {
        doNothing().when(service).deletar(1L);

        ResponseEntity<Void> resposta = controller.deletar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(204);
        verify(service, times(1)).deletar(1L);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverAnunciosAtivos() {
        when(service.listarAtivos()).thenReturn(List.of());

        ResponseEntity<List<AnuncioDTO>> resposta = controller.listarAtivos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isEmpty();
    }

    // ========================
    // CENÁRIOS INFELIZES
    // ========================

    @Test
    void deveLancarExcecaoQuandoAnuncioNaoEncontrado() {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Anúncio não encontrado com id: 99"));

        assertThatThrownBy(() -> controller.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anúncio não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaCriarAnuncio() {
        when(service.criar(any()))
                .thenThrow(new IllegalArgumentException("Apenas vendedores podem criar anúncios"));

        assertThatThrownBy(() -> controller.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas vendedores podem criar anúncios");
    }

    @Test
    void deveLancarExcecaoAoMarcarAnuncioJaVendido() {
        when(service.marcarComoVendido(1L))
                .thenThrow(new IllegalArgumentException("Anúncio já está marcado como vendido"));

        assertThatThrownBy(() -> controller.marcarComoVendido(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Anúncio já está marcado como vendido");
    }

    @Test
    void deveLancarExcecaoAoDeletarAnuncioInexistente() {
        doThrow(new ResourceNotFoundException("Anúncio não encontrado com id: 99"))
                .when(service).deletar(99L);

        assertThatThrownBy(() -> controller.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anúncio não encontrado com id: 99");
    }
}