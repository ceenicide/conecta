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
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.dto.DemandaRequest;
import com.feira.conecta.dto.DemandaResponse;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.DemandaService;

@ExtendWith(MockitoExtension.class)
class DemandaControllerTest {

    @Mock private DemandaService service;

    @InjectMocks
    private DemandaController controller;

    private DemandaRequest request;
    private DemandaResponse response;
    private OfertaFuturaDTO ofertaDto;

    @BeforeEach
    void setup() {
        request = new DemandaRequest(1L, new BigDecimal("200"), LocalDate.of(2026, 5, 13));

        response = new DemandaResponse(
                1L, 2L, "Carlos", 1L, "Soja",
                new BigDecimal("200"), LocalDate.of(2026, 5, 13),
                StatusDemanda.PROCURANDO
        );

        ofertaDto = OfertaFuturaDTO.builder()
                .id(1L).usuarioId(1L).usuarioNome("Maria")
                .produtoId(1L).produtoNome("Soja")
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.of(2026, 4, 30))
                .status(StatusOferta.ABERTA).build();
    }

    @Test
    void deveCriarDemandaERetornar201() {
        when(service.criar(any())).thenReturn(response);

        ResponseEntity<DemandaResponse> resposta = controller.criar(request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(resposta.getBody().status()).isEqualTo(StatusDemanda.PROCURANDO);
        assertThat(resposta.getBody().compradorNome()).isEqualTo("Carlos");
    }

    @Test
    void deveListarDemandasProcurandoERetornar200() {
        when(service.listarProcurando()).thenReturn(List.of(response));

        ResponseEntity<List<DemandaResponse>> resposta = controller.listarProcurando();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveListarMinhasDemandasERetornar200() {
        when(service.listarMinhasDemandas()).thenReturn(List.of(response));

        ResponseEntity<List<DemandaResponse>> resposta = controller.listarMinhasDemandas();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveListarOfertasCompativeisERetornar200() {
        when(service.listarOfertasCompativeis(1L)).thenReturn(List.of(ofertaDto));

        ResponseEntity<List<OfertaFuturaDTO>> resposta = controller.listarOfertasCompativeis(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getBody().get(0).getDataDisponivel())
                .isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(resposta.getBody().get(0).getUsuarioNome()).isEqualTo("Maria");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaOfertasCompativeis() {
        when(service.listarOfertasCompativeis(1L)).thenReturn(List.of());

        ResponseEntity<List<OfertaFuturaDTO>> resposta = controller.listarOfertasCompativeis(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoCompradorNaoTemPermissaoNasOfertas() {
        when(service.listarOfertasCompativeis(1L))
                .thenThrow(new IllegalArgumentException("Você não tem permissão para acessar esta demanda"));

        assertThatThrownBy(() -> controller.listarOfertasCompativeis(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para acessar esta demanda");
    }

    @Test
    void deveLancarExcecaoQuandoDemandaNaoExisteNaBuscaDeOfertas() {
        when(service.listarOfertasCompativeis(99L))
                .thenThrow(new ResourceNotFoundException("Demanda não encontrada com id: 99"));

        assertThatThrownBy(() -> controller.listarOfertasCompativeis(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Demanda não encontrada com id: 99");
    }
}