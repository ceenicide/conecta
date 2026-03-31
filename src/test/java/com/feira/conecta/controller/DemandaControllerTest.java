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

import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.dto.DemandaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.DemandaService;

@ExtendWith(MockitoExtension.class)
class DemandaControllerTest {

    @Mock private DemandaService service;
    @InjectMocks private DemandaController controller;

    private DemandaDTO dto;

    @BeforeEach
    void setup() {
        dto = DemandaDTO.builder()
                .id(1L).compradorId(2L).compradorNome("Carlos")
                .produtoId(1L).produtoNome("Soja")
                .quantidade(new BigDecimal("200"))
                .dataLimite(LocalDate.now().plusMonths(3))
                .status(StatusDemanda.PROCURANDO)
                .build();
    }

    @Test
    void deveCriarDemandaERetornar200() {
        when(service.criar(any())).thenReturn(dto);
        ResponseEntity<DemandaDTO> resposta = controller.criar(dto);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getStatus()).isEqualTo(StatusDemanda.PROCURANDO);
        assertThat(resposta.getBody().getCompradorNome()).isEqualTo("Carlos");
    }

    @Test
    void deveListarDemandasProcurandoERetornar200() {
        when(service.listarProcurando()).thenReturn(List.of(dto));
        ResponseEntity<List<DemandaDTO>> resposta = controller.listarProcurando();
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveListarDemandasPorCompradorERetornar200() {
        when(service.listarPorComprador(2L)).thenReturn(List.of(dto));
        ResponseEntity<List<DemandaDTO>> resposta = controller.listarPorComprador(2L);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().get(0).getCompradorId()).isEqualTo(2L);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverDemandas() {
        when(service.listarProcurando()).thenReturn(List.of());
        assertThat(controller.listarProcurando().getBody()).isEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoVendedorTentaCriarDemanda() {
        when(service.criar(any()))
                .thenThrow(new IllegalArgumentException("Apenas compradores podem criar demandas"));
        assertThatThrownBy(() -> controller.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas compradores podem criar demandas");
    }

    @Test
    void deveLancarExcecaoQuandoCompradorNaoEncontrado() {
        when(service.criar(any()))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado com id: 99"));
        assertThatThrownBy(() -> controller.criar(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }
}