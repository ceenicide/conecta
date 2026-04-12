package com.feira.conecta.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.AnuncioDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.AnuncioRepository;
import com.feira.conecta.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class AnuncioServiceTest {

    @Mock private AnuncioRepository anuncioRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private SecurityUtils securityUtils;
    // NOVO mock necessário após injeção do AnuncioMatchingService no AnuncioService
    @Mock private AnuncioMatchingService anuncioMatchingService;

    @InjectMocks
    private AnuncioService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Produto produto;
    private Anuncio anuncio;
    private AnuncioDTO dto;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("11999990000")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("11888880000")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder()
                .id(1L).nome("Soja").descricao("Safra 2025").usuario(vendedor).build();

        anuncio = Anuncio.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("100"))
                .preco(new BigDecimal("50.00"))
                .status(StatusAnuncio.ATIVO)
                .build();

        dto = AnuncioDTO.builder()
                .produtoId(1L)
                .quantidade(new BigDecimal("100"))
                .preco(new BigDecimal("50.00"))
                .build();
    }

    @Test
    void deveCriarAnuncioComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(anuncioRepository.save(any())).thenReturn(anuncio);
        // executarMatching é @Async — void, sem retorno; o mock já ignora por padrão
        doNothing().when(anuncioMatchingService).executarMatching(any());

        AnuncioDTO resultado = service.criar(dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusAnuncio.ATIVO);
        assertThat(resultado.getUsuarioNome()).isEqualTo("Maria");
        assertThat(resultado.getProdutoNome()).isEqualTo("Soja");
        verify(anuncioRepository, times(1)).save(any());
        // Garante que o gatilho de matching foi disparado após salvar
        verify(anuncioMatchingService, times(1)).executarMatching(any());
    }

    @Test
    void deveDispararMatchingAposSalvarAnuncio() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(anuncioRepository.save(any())).thenReturn(anuncio);
        doNothing().when(anuncioMatchingService).executarMatching(any());

        service.criar(dto);

        // O matching deve ser disparado exatamente uma vez por criação
        verify(anuncioMatchingService, times(1)).executarMatching(anuncio);
    }

    @Test
    void deveListarAnunciosAtivos() {
        when(anuncioRepository.findByStatus(StatusAnuncio.ATIVO)).thenReturn(List.of(anuncio));

        List<AnuncioDTO> resultado = service.listarAtivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getStatus()).isEqualTo(StatusAnuncio.ATIVO);
    }

    @Test
    void deveBuscarAnuncioPorId() {
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        AnuncioDTO resultado = service.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getPreco()).isEqualByComparingTo("50.00");
    }

    @Test
    void deveListarMeusAnuncios() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(anuncioRepository.findByUsuarioId(1L)).thenReturn(List.of(anuncio));

        List<AnuncioDTO> resultado = service.listarMeusAnuncios();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void deveMarcarAnuncioComoVendido() {
        Anuncio vendido = Anuncio.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("100")).preco(new BigDecimal("50.00"))
                .status(StatusAnuncio.VENDIDO).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        when(anuncioRepository.save(any())).thenReturn(vendido);

        AnuncioDTO resultado = service.marcarComoVendido(1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusAnuncio.VENDIDO);
    }

    @Test
    void deveDeletarAnuncioComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        doNothing().when(anuncioRepository).deleteById(1L);

        assertThatCode(() -> service.deletar(1L)).doesNotThrowAnyException();
        verify(anuncioRepository, times(1)).deleteById(1L);
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaCriarAnuncio() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas vendedores podem criar anúncios");
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
    void deveLancarExcecaoAoMarcarAnuncioJaVendido() {
        anuncio.setStatus(StatusAnuncio.VENDIDO);
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.marcarComoVendido(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Anúncio já está marcado como vendido");
    }

    @Test
    void deveLancarExcecaoAoMarcarAnuncioDeOutroVendedor() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.marcarComoVendido(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para alterar este anúncio");
    }

    @Test
    void deveLancarExcecaoAoDeletarAnuncioDeOutroVendedor() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.deletar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para deletar este anúncio");
    }

    @Test
    void deveLancarExcecaoAoBuscarAnuncioInexistente() {
        when(anuncioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Anúncio não encontrado com id: 99");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverAnunciosAtivos() {
        when(anuncioRepository.findByStatus(StatusAnuncio.ATIVO)).thenReturn(List.of());

        assertThat(service.listarAtivos()).isEmpty();
    }
}