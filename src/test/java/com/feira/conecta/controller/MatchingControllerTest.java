package com.feira.conecta.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.domain.StatusMatching;
import com.feira.conecta.dto.MatchingDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.MatchingService;

@ExtendWith(MockitoExtension.class)
class MatchingControllerTest {

    @Mock private MatchingService service;
    @InjectMocks private MatchingController controller;

    private MatchingDTO dto;

    @BeforeEach
    void setup() {
        dto = MatchingDTO.builder()
                .id(1L).ofertaId(1L).vendedorNome("Maria")
                .produtoNome("Soja").demandaId(1L).compradorNome("Carlos")
                .status(StatusMatching.PENDENTE)
                .build();
    }

    @Test
    void deveListarMatchingsPorOfertaERetornar200() {
        when(service.listarPorOferta(1L)).thenReturn(List.of(dto));
        ResponseEntity<List<MatchingDTO>> resposta = controller.listarPorOferta(1L);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).getVendedorNome()).isEqualTo("Maria");
    }

    @Test
    void deveListarMatchingsPorDemandaERetornar200() {
        when(service.listarPorDemanda(1L)).thenReturn(List.of(dto));
        ResponseEntity<List<MatchingDTO>> resposta = controller.listarPorDemanda(1L);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().get(0).getCompradorNome()).isEqualTo("Carlos");
    }

    @Test
    void deveAceitarMatchingERetornar200() {
        MatchingDTO aceito = MatchingDTO.builder()
                .id(1L).status(StatusMatching.ACEITO).build();
        when(service.aceitar(1L)).thenReturn(aceito);
        ResponseEntity<MatchingDTO> resposta = controller.aceitar(1L);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusMatching.ACEITO);
    }

    @Test
    void deveRecusarMatchingERetornar200() {
        MatchingDTO recusado = MatchingDTO.builder()
                .id(1L).status(StatusMatching.RECUSADO).build();
        when(service.recusar(1L)).thenReturn(recusado);
        ResponseEntity<MatchingDTO> resposta = controller.recusar(1L);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusMatching.RECUSADO);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverMatchings() {
        when(service.listarPorOferta(1L)).thenReturn(List.of());
        assertThat(controller.listarPorOferta(1L).getBody()).isEmpty();
    }

    @Test
    void deveLancarExcecaoAoAceitarMatchingNaoPendente() {
        when(service.aceitar(1L))
                .thenThrow(new IllegalArgumentException("Apenas matchings pendentes podem ser aceitos"));
        assertThatThrownBy(() -> controller.aceitar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas matchings pendentes podem ser aceitos");
    }

    @Test
    void deveLancarExcecaoQuandoMatchingNaoEncontrado() {
        when(service.aceitar(99L))
                .thenThrow(new ResourceNotFoundException("Matching não encontrado com id: 99"));
        assertThatThrownBy(() -> controller.aceitar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Matching não encontrado com id: 99");
    }
}