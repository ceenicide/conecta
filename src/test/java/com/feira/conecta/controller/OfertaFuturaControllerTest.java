package com.feira.conecta.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.OfertaFuturaService;

@ExtendWith(MockitoExtension.class)
class OfertaFuturaControllerTest {

    @Mock private OfertaFuturaService service;
    @InjectMocks private OfertaFuturaController controller;

    private OfertaFuturaDTO dto;

    @BeforeEach
    void setup() {
        dto = OfertaFuturaDTO.builder()
                .id(1L).usuarioId(1L).usuarioNome("Maria")
                .produtoId(1L).produtoNome("Soja")
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.now().plusMonths(2))
                .status(StatusOferta.ABERTA)
                .build();
    }

    @Test
    void deveCriarOfertaERetornar200() {
        when(service.criar(any())).thenReturn(dto);
        ResponseEntity<OfertaFuturaDTO> resposta = controller.criar(dto);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusOferta.ABERTA);
        assertThat(resposta.getBody().getUsuarioNome()).isEqualTo("Maria");
    }

    @Test
    void deveListarOfertasAbertasERetornar200() {
        when(service.listarAbertas()).thenReturn(List.of(dto));
        ResponseEntity<List<OfertaFuturaDTO>> resposta = controller.listarAbertas();
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveListarOfertasPorUsuarioERetornar200() {
        when(service.listarPorUsuario(1L)).thenReturn(List.of(dto));
        ResponseEntity<List<OfertaFuturaDTO>> resposta = controller.listarPorUsuario(1L);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().get(0).getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverOfertas() {
        when(service.listarAbertas()).thenReturn(List.of());
        assertThat(controller.listarAbertas().getBody()).isEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaCriarOferta() {
        when(service.criar(any()))
                .thenThrow(new IllegalArgumentException("Apenas vendedores podem criar ofertas futuras"));
        assertThatThrownBy(() -> controller.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas vendedores podem criar ofertas futuras");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(service.criar(any()))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado com id: 99"));
        assertThatThrownBy(() -> controller.criar(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }
}