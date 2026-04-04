package com.feira.conecta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.feira.conecta.domain.Demanda;
import com.feira.conecta.domain.Matching;
import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.domain.StatusMatching;
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.MatchingDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.DemandaRepository;
import com.feira.conecta.repository.MatchingRepository;
import com.feira.conecta.repository.OfertaFuturaRepository;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock private MatchingRepository matchingRepository;
    @Mock private OfertaFuturaRepository ofertaRepository;
    @Mock private DemandaRepository demandaRepository;

    @InjectMocks
    private MatchingService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Produto produto;
    private OfertaFutura oferta;
    private Demanda demanda;
    private Matching matching;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder()
                .id(1L).nome("Soja").build();

        oferta = OfertaFutura.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.now().plusMonths(2))
                .status(StatusOferta.ABERTA)
                .build();

        demanda = Demanda.builder()
                .id(1L).comprador(comprador).produto(produto)
                .quantidade(new BigDecimal("200"))
                .dataLimite(LocalDate.now().plusMonths(3))
                .status(StatusDemanda.PROCURANDO)
                .build();

        matching = Matching.builder()
                .id(1L).oferta(oferta).demanda(demanda)
                .status(StatusMatching.PENDENTE)
                .build();
    }

    // CENÁRIOS FELIZES

    @Test
    void deveCriarMatchingQuandoOfertaEDemandaSaoCompativeis() {
        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of(demanda));
        when(matchingRepository.existsByOfertaIdAndDemandaId(1L, 1L)).thenReturn(false);
        when(matchingRepository.save(any())).thenReturn(matching);

        service.buscarMatchesPorOferta(oferta);

        verify(matchingRepository, times(1)).save(any(Matching.class));
    }

    @Test
    void deveCriarMatchingQuandoDemandaEOfertaSaoCompativeis() {
        when(ofertaRepository.findByProdutoAndStatusAndDataDisponivelLessThanEqual(
                produto, StatusOferta.ABERTA, demanda.getDataLimite()))
                .thenReturn(List.of(oferta));
        when(matchingRepository.existsByOfertaIdAndDemandaId(1L, 1L)).thenReturn(false);
        when(matchingRepository.save(any())).thenReturn(matching);

        service.buscarMatchesPorDemanda(demanda);

        verify(matchingRepository, times(1)).save(any(Matching.class));
    }

    @Test
    void deveAceitarMatchingEFecharOfertaEAtenderDemanda() {
        when(matchingRepository.findById(1L)).thenReturn(Optional.of(matching));
        when(matchingRepository.save(any())).thenReturn(matching);

        MatchingDTO resultado = service.aceitar(1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusMatching.ACEITO);
        assertThat(oferta.getStatus()).isEqualTo(StatusOferta.FECHADA);
        assertThat(demanda.getStatus()).isEqualTo(StatusDemanda.ATENDIDA);
        verify(ofertaRepository, times(1)).save(oferta);
        verify(demandaRepository, times(1)).save(demanda);
    }

    @Test
    void deveRecusarMatchingSemAlterarOfertaEDemanda() {
        when(matchingRepository.findById(1L)).thenReturn(Optional.of(matching));
        when(matchingRepository.save(any())).thenReturn(
                Matching.builder().id(1L).oferta(oferta).demanda(demanda)
                        .status(StatusMatching.RECUSADO).build());

        MatchingDTO resultado = service.recusar(1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusMatching.RECUSADO);
        assertThat(oferta.getStatus()).isEqualTo(StatusOferta.ABERTA);
        assertThat(demanda.getStatus()).isEqualTo(StatusDemanda.PROCURANDO);
        verify(ofertaRepository, never()).save(any());
        verify(demandaRepository, never()).save(any());
    }

    @Test
    void deveListarMatchingsPorOferta() {
        when(matchingRepository.findByOfertaId(1L)).thenReturn(List.of(matching));

        List<MatchingDTO> resultado = service.listarPorOferta(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getOfertaId()).isEqualTo(1L);
    }

    @Test
    void deveListarMatchingsPorDemanda() {
        when(matchingRepository.findByDemandaId(1L)).thenReturn(List.of(matching));

        List<MatchingDTO> resultado = service.listarPorDemanda(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDemandaId()).isEqualTo(1L);
    }

    @Test
    void naoDeveCriarMatchingDuplicado() {
        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of(demanda));
        when(matchingRepository.existsByOfertaIdAndDemandaId(1L, 1L)).thenReturn(true);

        service.buscarMatchesPorOferta(oferta);

        verify(matchingRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarMatchingQuandoQuantidadeInsuficiente() {
        Demanda demandaGrande = Demanda.builder()
                .id(2L).comprador(comprador).produto(produto)
                .quantidade(new BigDecimal("9999"))
                .dataLimite(LocalDate.now().plusMonths(3))
                .status(StatusDemanda.PROCURANDO)
                .build();

        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of(demandaGrande));

        service.buscarMatchesPorOferta(oferta);

        verify(matchingRepository, never()).save(any());
    }

    // CENÁRIOS INFELIZES

    @Test
    void deveLancarExcecaoAoAceitarMatchingNaoPendente() {
        matching.setStatus(StatusMatching.ACEITO);
        when(matchingRepository.findById(1L)).thenReturn(Optional.of(matching));

        assertThatThrownBy(() -> service.aceitar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas matchings pendentes podem ser aceitos");
    }

    @Test
    void deveLancarExcecaoAoRecusarMatchingNaoPendente() {
        matching.setStatus(StatusMatching.RECUSADO);
        when(matchingRepository.findById(1L)).thenReturn(Optional.of(matching));

        assertThatThrownBy(() -> service.recusar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas matchings pendentes podem ser recusados");
    }

    @Test
    void deveLancarExcecaoQuandoMatchingNaoEncontrado() {
        when(matchingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aceitar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Matching não encontrado com id: 99");
    }
}