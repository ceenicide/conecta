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

import com.feira.conecta.domain.OfertaFutura;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusOferta;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.OfertaFuturaRepository;
import com.feira.conecta.repository.ProdutoRepository;
import com.feira.conecta.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class OfertaFuturaServiceTest {

    @Mock private OfertaFuturaRepository repository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private MatchingService matchingService;

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
                .tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("22222222222")
                .tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder()
                .id(1L).nome("Soja").descricao("Safra 2026").build();

        oferta = OfertaFutura.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.now().plusMonths(2))
                .status(StatusOferta.ABERTA)
                .build();

        dto = OfertaFuturaDTO.builder()
                .usuarioId(1L).produtoId(1L)
                .quantidade(new BigDecimal("500"))
                .dataDisponivel(LocalDate.now().plusMonths(2))
                .build();
    }

    // CENÁRIOS FELIZES

    @Test
    void deveCriarOfertaFuturaComSucesso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.save(any())).thenReturn(oferta);

        OfertaFuturaDTO resultado = service.criar(dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusOferta.ABERTA);
        assertThat(resultado.getUsuarioNome()).isEqualTo("Maria");
        assertThat(resultado.getProdutoNome()).isEqualTo("Soja");
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
    void deveListarOfertasPorUsuario() {
        when(repository.findByUsuarioId(1L)).thenReturn(List.of(oferta));

        List<OfertaFuturaDTO> resultado = service.listarPorUsuario(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverOfertas() {
        when(repository.findByStatus(StatusOferta.ABERTA)).thenReturn(List.of());

        assertThat(service.listarAbertas()).isEmpty();
    }

    @Test
    void deveChamarMatchingServiceAoCriarOferta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.save(any())).thenReturn(oferta);

        service.criar(dto);

        verify(matchingService, times(1)).buscarMatchesPorOferta(oferta);
    }

    // CENÁRIOS INFELIZES

    @Test
    void deveLancarExcecaoQuandoCompradorTentaCriarOferta() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(comprador));

        OfertaFuturaDTO dtoInvalido = OfertaFuturaDTO.builder()
                .usuarioId(2L).produtoId(1L)
                .quantidade(new BigDecimal("100"))
                .dataDisponivel(LocalDate.now().plusMonths(1))
                .build();

        assertThatThrownBy(() -> service.criar(dtoInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas vendedores podem criar ofertas futuras");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        OfertaFuturaDTO dtoInvalido = OfertaFuturaDTO.builder()
                .usuarioId(99L).produtoId(1L)
                .quantidade(new BigDecimal("100"))
                .dataDisponivel(LocalDate.now().plusMonths(1))
                .build();

        assertThatThrownBy(() -> service.criar(dtoInvalido))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        OfertaFuturaDTO dtoInvalido = OfertaFuturaDTO.builder()
                .usuarioId(1L).produtoId(99L)
                .quantidade(new BigDecimal("100"))
                .dataDisponivel(LocalDate.now().plusMonths(1))
                .build();

        assertThatThrownBy(() -> service.criar(dtoInvalido))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Produto não encontrado com id: 99");
    }
}