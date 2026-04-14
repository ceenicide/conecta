package com.feira.conecta.service;

import java.math.BigDecimal;
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
import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Pedido;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.StatusPedido;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.PedidoRequest;
import com.feira.conecta.dto.PedidoResponse;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.AnuncioRepository;
import com.feira.conecta.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private AnuncioRepository anuncioRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private PedidoService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Produto produto;
    private Anuncio anuncio;
    private Pedido pedido;
    private PedidoRequest request;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("(11) 999990000")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("(11) 888880000")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder().id(1L).nome("Soja").build();

        anuncio = Anuncio.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("100"))
                .preco(new BigDecimal("50.00"))
                .status(StatusAnuncio.ATIVO).build();

        pedido = Pedido.builder()
                .id(1L).comprador(comprador).anuncio(anuncio)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.PENDENTE).build();

        // Request: só anuncioId e quantidade — compradorId vem do token
        request = new PedidoRequest(1L, new BigDecimal("10"));
    }

    @Test
    void deveCriarPedidoComSucesso() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        when(pedidoRepository.save(any())).thenReturn(pedido);

        PedidoResponse resultado = service.criar(request);

        assertThat(resultado.status()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(resultado.compradorNome()).isEqualTo("Carlos");
        assertThat(resultado.vendedorNome()).isEqualTo("Maria");
        verify(pedidoRepository, times(1)).save(any());
    }

    @Test
    void deveBuscarPedidoPorIdComoComprador() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoResponse resultado = service.buscarPorId(1L);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.quantidade()).isEqualByComparingTo("10");
    }

    @Test
    void deveBuscarPedidoPorIdComoVendedor() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoResponse resultado = service.buscarPorId(1L);

        assertThat(resultado.id()).isEqualTo(1L);
    }

    @Test
    void deveListarMeusPedidos() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(pedidoRepository.findByCompradorId(2L)).thenReturn(List.of(pedido));

        List<PedidoResponse> resultado = service.listarMeusPedidos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).compradorId()).isEqualTo(2L);
    }

    @Test
    void deveConfirmarPedidoComoVendedor() {
        Pedido confirmado = Pedido.builder()
                .id(1L).comprador(comprador).anuncio(anuncio)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.CONFIRMADO).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenReturn(confirmado);

        PedidoResponse resultado = service.confirmar(1L);

        assertThat(resultado.status()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.VENDIDO);
        verify(anuncioRepository, times(1)).save(anuncio);
    }

    @Test
    void deveFinalizarPedidoComoComprador() {
        pedido.setStatus(StatusPedido.CONFIRMADO);
        Pedido finalizado = Pedido.builder()
                .id(1L).comprador(comprador).anuncio(anuncio)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.FINALIZADO).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenReturn(finalizado);

        PedidoResponse resultado = service.finalizar(1L);

        assertThat(resultado.status()).isEqualTo(StatusPedido.FINALIZADO);
    }

    @Test
    void deveLancarExcecaoQuandoVendedorTentaFazerPedido() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas compradores podem fazer pedidos");
    }

    @Test
    void deveLancarExcecaoQuandoAnuncioEstaVendido() {
        anuncio.setStatus(StatusAnuncio.VENDIDO);
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este anúncio não está disponível");
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaComprarProprioAnuncio() {
        anuncio.setUsuario(comprador);
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não pode fazer pedido do seu próprio anúncio");
    }

    @Test
    void deveLancarExcecaoQuandoQuantidadeSuperiorAoDisponivel() {
        PedidoRequest excesso = new PedidoRequest(1L, new BigDecimal("999"));
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.criar(excesso))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade solicitada maior que a disponível no anúncio");
    }

    @Test
    void deveLancarExcecaoAoConfirmarPedidoComoComprador() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.confirmar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para confirmar este pedido");
    }

    @Test
    void deveLancarExcecaoAoFinalizarPedidoComoVendedor() {
        pedido.setStatus(StatusPedido.CONFIRMADO);
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.finalizar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para finalizar este pedido");
    }

    @Test
    void deveLancarExcecaoAoConfirmarPedidoNaoPendente() {
        pedido.setStatus(StatusPedido.CONFIRMADO);
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.confirmar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas pedidos pendentes podem ser confirmados");
    }

    @Test
    void deveLancarExcecaoAoBuscarPedidoDeOutroUsuario() {
        Usuario outro = Usuario.builder()
                .id(99L).nome("Outro").telefone("(99) 999999999")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(outro);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.buscarPorId(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para visualizar este pedido");
    }

    @Test
    void deveLancarExcecaoAoBuscarPedidoInexistente() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Pedido não encontrado com id: 99");
    }
}