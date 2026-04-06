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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Demanda;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.DemandaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.DemandaRepository;
import com.feira.conecta.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class DemandaServiceTest {

    @Mock private DemandaRepository repository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private MatchingService matchingService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private DemandaService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Produto produto;
    private Demanda demanda;
    private DemandaDTO dto;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("11111111111")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("22222222222")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder().id(1L).nome("Soja").build();

        demanda = Demanda.builder()
                .id(1L).comprador(comprador).produto(produto)
                .quantidade(new BigDecimal("200"))
                .dataLimite(LocalDate.now().plusMonths(3))
                .status(StatusDemanda.PROCURANDO).build();

        // dto sem compradorId — vem do token
        dto = DemandaDTO.builder()
                .produtoId(1L)
                .quantidade(new BigDecimal("200"))
                .dataLimite(LocalDate.now().plusMonths(3))
                .build();
    }

    @Test
    void deveCriarDemandaComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.save(any())).thenReturn(demanda);

        DemandaDTO resultado = service.criar(dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusDemanda.PROCURANDO);
        assertThat(resultado.getCompradorNome()).isEqualTo("Carlos");
        verify(matchingService, times(1)).buscarMatchesPorDemanda(any());
    }

    @Test
    void deveListarDemandasProcurando() {
        when(repository.findByStatus(StatusDemanda.PROCURANDO)).thenReturn(List.of(demanda));

        List<DemandaDTO> resultado = service.listarProcurando();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getStatus()).isEqualTo(StatusDemanda.PROCURANDO);
    }

    @Test
    void deveListarMinhasDemandas() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(repository.findByCompradorId(2L)).thenReturn(List.of(demanda));

        List<DemandaDTO> resultado = service.listarMinhasDemandas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCompradorId()).isEqualTo(2L);
    }

    @Test
    void deveLancarExcecaoQuandoVendedorTentaCriarDemanda() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas compradores podem criar demandas");
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(produtoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 1");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverDemandas() {
        when(repository.findByStatus(StatusDemanda.PROCURANDO)).thenReturn(List.of());

        assertThat(service.listarProcurando()).isEmpty();
    }
}
