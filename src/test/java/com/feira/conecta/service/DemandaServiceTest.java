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
import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.DemandaDTO;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.DemandaRepository;
import com.feira.conecta.repository.OfertaFuturaRepository;
import com.feira.conecta.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class DemandaServiceTest {

    @Mock private DemandaRepository repository;
    @Mock private ProdutoRepository produtoRepository;
    // FIX: novo campo injetado no DemandaService — necessário para listarOfertasCompativeis
    @Mock private OfertaFuturaRepository ofertaFuturaRepository;
    @Mock private MatchingService matchingService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private DemandaService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Usuario outroComprador;
    private Produto produto;
    private Demanda demanda;
    private OfertaFutura oferta;
    private DemandaDTO dto;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("(11) 999999999")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("(11) 888888888")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        outroComprador = Usuario.builder()
                .id(3L).nome("Ana").telefone("(11) 777777777")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder()
                .id(1L).nome("Soja").usuario(vendedor).build();

        demanda = Demanda.builder()
                .id(1L).comprador(comprador).produto(produto)
                .quantidade(new BigDecimal("200"))
                .dataLimite(LocalDate.of(2026, 5, 13))
                .status(StatusDemanda.PROCURANDO).build();

        oferta = OfertaFutura.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.of(2026, 4, 30))
                .status(StatusOferta.ABERTA).build();

        dto = DemandaDTO.builder()
                .produtoId(1L)
                .quantidade(new BigDecimal("200"))
                .dataLimite(LocalDate.of(2026, 5, 13))
                .build();
    }

    // ========================
    // CRIAR
    // ========================

    @Test
    void deveCriarDemandaComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.save(any())).thenReturn(demanda);

        DemandaDTO resultado = service.criar(dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusDemanda.PROCURANDO);
        assertThat(resultado.getCompradorNome()).isEqualTo("Carlos");
        assertThat(resultado.getCompradorId()).isEqualTo(2L);
        verify(matchingService, times(1)).buscarMatchesPorDemanda(any());
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

    // ========================
    // LISTAR
    // ========================

    @Test
    void deveListarDemandasProcurando() {
        when(repository.findByStatus(StatusDemanda.PROCURANDO)).thenReturn(List.of(demanda));

        List<DemandaDTO> resultado = service.listarProcurando();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getStatus()).isEqualTo(StatusDemanda.PROCURANDO);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverDemandas() {
        when(repository.findByStatus(StatusDemanda.PROCURANDO)).thenReturn(List.of());

        assertThat(service.listarProcurando()).isEmpty();
    }

    @Test
    void deveListarMinhasDemandas() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(repository.findByCompradorId(2L)).thenReturn(List.of(demanda));

        List<DemandaDTO> resultado = service.listarMinhasDemandas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCompradorId()).isEqualTo(2L);
    }

    // ========================
    // LISTAR OFERTAS COMPATÍVEIS (novo método)
    // ========================

    @Test
    void deveListarOfertasCompativeisComSucesso() {
        // Demanda com prazo 13/05/2026 — oferta disponível em 30/04/2026 deve aparecer
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(repository.findById(1L)).thenReturn(Optional.of(demanda));
        when(ofertaFuturaRepository.findByStatusAndDataDisponivelLessThanEqual(
                StatusOferta.ABERTA, demanda.getDataLimite()))
                .thenReturn(List.of(oferta));

        List<OfertaFuturaDTO> resultado = service.listarOfertasCompativeis(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDataDisponivel()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(resultado.get(0).getStatus()).isEqualTo(StatusOferta.ABERTA);
        assertThat(resultado.get(0).getUsuarioNome()).isEqualTo("Maria");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaOfertasDentroDoPrazo() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(repository.findById(1L)).thenReturn(Optional.of(demanda));
        when(ofertaFuturaRepository.findByStatusAndDataDisponivelLessThanEqual(
                StatusOferta.ABERTA, demanda.getDataLimite()))
                .thenReturn(List.of());

        List<OfertaFuturaDTO> resultado = service.listarOfertasCompativeis(1L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoOutroCompradorTentaVerOfertasCompativeis() {
        // outroComprador não é dono da demanda — deve ser barrado
        when(securityUtils.getUsuarioLogado()).thenReturn(outroComprador);
        when(repository.findById(1L)).thenReturn(Optional.of(demanda));

        assertThatThrownBy(() -> service.listarOfertasCompativeis(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para acessar esta demanda");
    }

    @Test
    void deveLancarExcecaoQuandoDemandaNaoExisteAoBuscarOfertas() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarOfertasCompativeis(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Demanda não encontrada com id: 99");
    }
}