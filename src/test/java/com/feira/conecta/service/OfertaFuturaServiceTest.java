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
import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.OfertaFuturaRepository;
import com.feira.conecta.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class OfertaFuturaServiceTest {

    @Mock private OfertaFuturaRepository repository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private MatchingService matchingService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private OfertaFuturaService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Produto produto;
    private OfertaFutura oferta;
    private OfertaFuturaDTO dto;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("11111111111")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("22222222222")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder().id(1L).nome("Soja").build();

        oferta = OfertaFutura.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.now().plusMonths(2))
                .status(StatusOferta.ABERTA).build();

        // dto sem usuarioId — vem do token
        dto = OfertaFuturaDTO.builder()
                .produtoId(1L)
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.now().plusMonths(2))
                .build();
    }

    @Test
    void deveCriarOfertaFuturaComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.save(any())).thenReturn(oferta);

        OfertaFuturaDTO resultado = service.criar(dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusOferta.ABERTA);
        assertThat(resultado.getUsuarioNome()).isEqualTo("Maria");
        verify(matchingService, times(1)).buscarMatchesPorOferta(any());
    }

    @Test
    void deveListarOfertasAbertas() {
        when(repository.findByStatus(StatusOferta.ABERTA)).thenReturn(List.of(oferta));

        List<OfertaFuturaDTO> resultado = service.listarAbertas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getStatus()).isEqualTo(StatusOferta.ABERTA);
    }

    @Test
    void deveListarMinhasOfertas() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(repository.findByUsuarioId(1L)).thenReturn(List.of(oferta));

        List<OfertaFuturaDTO> resultado = service.listarMinhasOfertas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaCriarOferta() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas vendedores podem criar ofertas futuras");
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(produtoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 1");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverOfertas() {
        when(repository.findByStatus(StatusOferta.ABERTA)).thenReturn(List.of());

        assertThat(service.listarAbertas()).isEmpty();
    }
}
