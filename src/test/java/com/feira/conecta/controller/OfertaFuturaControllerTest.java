package com.feira.conecta.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.feira.conecta.service.OfertaFuturaService;

@ExtendWith(MockitoExtension.class)
class OfertaFuturaControllerTest {

    @Mock private OfertaFuturaService service;

    @InjectMocks
    private OfertaFuturaController controller;

    private OfertaFuturaDTO dto;

    @BeforeEach
    void setup() {
        dto = OfertaFuturaDTO.builder()
                .id(1L).usuarioId(1L).usuarioNome("Maria")
                .produtoId(1L).produtoNome("Soja")
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.now().plusMonths(2))
                .status(StatusOferta.ABERTA).build();
    }

    @Test
    void deveCriarOfertaFuturaERetornar200() {
        when(service.criar(any())).thenReturn(dto);

        ResponseEntity<OfertaFuturaDTO> resposta = controller.criar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusOferta.ABERTA);
    }

    @Test
    void deveListarOfertasAbertasERetornar200() {
        when(service.listarAbertas()).thenReturn(List.of(dto));

        ResponseEntity<List<OfertaFuturaDTO>> resposta = controller.listarAbertas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveListarMinhasOfertasERetornar200() {
        when(service.listarMinhasOfertas()).thenReturn(List.of(dto));

        ResponseEntity<List<OfertaFuturaDTO>> resposta = controller.listarMinhasOfertas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }
}
