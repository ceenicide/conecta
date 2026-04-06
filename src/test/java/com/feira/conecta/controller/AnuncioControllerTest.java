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

    @Mock private AnuncioService service;

    @InjectMocks
    private AnuncioController controller;

    private AnuncioDTO dto;

    @BeforeEach
    void setup() {
        dto = AnuncioDTO.builder()
                .id(1L).usuarioId(1L).usuarioNome("Maria")
                .produtoId(1L).produtoNome("Soja")
                .quantidade(new BigDecimal("100"))
                .preco(new BigDecimal("50.00"))
                .status(StatusAnuncio.ATIVO).build();
    }

    @Test
    void deveCriarAnuncioERetornar200() {
        when(service.criar(any())).thenReturn(dto);

        ResponseEntity<AnuncioDTO> resposta = controller.criar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void deveListarAnunciosAtivosERetornar200() {
        when(service.listarAtivos()).thenReturn(List.of(dto));

        ResponseEntity<List<AnuncioDTO>> resposta = controller.listarAtivos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveBuscarAnuncioPorIdERetornar200() {
        when(service.buscarPorId(1L)).thenReturn(dto);

        ResponseEntity<AnuncioDTO> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getProdutoNome()).isEqualTo("Soja");
    }

    @Test
    void deveListarMeusAnunciosERetornar200() {
        when(service.listarMeusAnuncios()).thenReturn(List.of(dto));

        ResponseEntity<List<AnuncioDTO>> resposta = controller.listarMeusAnuncios();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveMarcarAnuncioComoVendidoERetornar200() {
        AnuncioDTO vendido = AnuncioDTO.builder()
                .id(1L).status(StatusAnuncio.VENDIDO).build();

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
    void deveLancarExcecaoQuandoAnuncioNaoEncontrado() {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Anúncio não encontrado com id: 99"));

        assertThatThrownBy(() -> controller.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anúncio não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoSemPermissaoParaDeletar() {
        doThrow(new IllegalArgumentException("Você não tem permissão para deletar este anúncio"))
                .when(service).deletar(1L);

        assertThatThrownBy(() -> controller.deletar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para deletar este anúncio");
    }
}
