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

import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Pedido;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.StatusPedido;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.PedidoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.AnuncioRepository;
import com.feira.conecta.repository.PedidoRepository;
import com.feira.conecta.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AnuncioRepository anuncioRepository;

    @InjectMocks
    private PedidoService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Produto produto;
    private Anuncio anuncio;
    private Pedido pedido;
    private PedidoDTO dto;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("11999990000")
                .tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("11888880000")
                .tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder()
                .id(1L).nome("Soja").descricao("Safra 2025").build();

        anuncio = Anuncio.builder()
                .id(1L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("100"))
                .preco(new BigDecimal("50.00"))
                .status(StatusAnuncio.ATIVO).build();

        pedido = Pedido.builder()
                .id(1L).comprador(comprador).anuncio(anuncio)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.PENDENTE).build();

        dto = PedidoDTO.builder()
                .compradorId(2L).anuncioId(1L)
                .quantidade(new BigDecimal("10")).build();
    }

    // CENÁRIOS FELIZES

    @Test
    void deveCriarPedidoComSucesso() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(comprador));
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        when(pedidoRepository.save(any())).thenReturn(pedido);

        PedidoDTO resultado = service.criar(dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(resultado.getCompradorNome()).isEqualTo("Carlos");
        assertThat(resultado.getVendedorNome()).isEqualTo("Maria");
        verify(pedidoRepository, times(1)).save(any());
    }

    @Test
    void deveBuscarPedidoPorId() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoDTO resultado = service.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getQuantidade()).isEqualByComparingTo("10");
    }

    @Test
    void deveListarPedidosPorComprador() {
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(pedidoRepository.findByCompradorId(2L)).thenReturn(List.of(pedido));

        List<PedidoDTO> resultado = service.listarPorComprador(2L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCompradorId()).isEqualTo(2L);
    }

    @Test
    void deveConfirmarPedidoEMarcarAnuncioComoVendido() {
        Pedido confirmado = Pedido.builder()
                .id(1L).comprador(comprador).anuncio(anuncio)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.CONFIRMADO).build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenReturn(confirmado);

        PedidoDTO resultado = service.confirmar(1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
        // verifica que o anúncio foi marcado como vendido
        assertThat(anuncio.getStatus()).isEqualTo(StatusAnuncio.VENDIDO);
        verify(anuncioRepository, times(1)).save(anuncio);
    }

    @Test
    void deveFinalizarPedidoConfirmado() {
        pedido.setStatus(StatusPedido.CONFIRMADO);
        Pedido finalizado = Pedido.builder()
                .id(1L).comprador(comprador).anuncio(anuncio)
                .quantidade(new BigDecimal("10"))
                .status(StatusPedido.FINALIZADO).build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenReturn(finalizado);

        PedidoDTO resultado = service.finalizar(1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.FINALIZADO);
    }

    @Test
    void deveRetornarListaVaziaQuandoCompradorNaoTiverPedidos() {
        when(usuarioRepository.existsById(2L)).thenReturn(true);
        when(pedidoRepository.findByCompradorId(2L)).thenReturn(List.of());

        assertThat(service.listarPorComprador(2L)).isEmpty();
    }

    // CENÁRIOS INFELIZES

    @Test
    void deveLancarExcecaoQuandoVendedorTentaFazerPedido() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));

        PedidoDTO dtoVendedor = PedidoDTO.builder()
                .compradorId(1L).anuncioId(1L)
                .quantidade(new BigDecimal("10")).build();

        assertThatThrownBy(() -> service.criar(dtoVendedor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas compradores podem fazer pedidos");
    }

    @Test
    void deveLancarExcecaoQuandoAnuncioEstaVendido() {
        anuncio.setStatus(StatusAnuncio.VENDIDO);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(comprador));
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este anúncio não está disponível");
    }

    @Test
    void deveLancarExcecaoQuandoCompradorTentaComprarProprioAnuncio() {
        // comprador é o mesmo que o dono do anúncio
        anuncio.setUsuario(comprador);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(comprador));
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não pode fazer pedido do seu próprio anúncio");
    }

    @Test
    void deveLancarExcecaoQuandoQuantidadeSuperiorAoDisponivel() {
        PedidoDTO dtoExcesso = PedidoDTO.builder()
                .compradorId(2L).anuncioId(1L)
                .quantidade(new BigDecimal("999")).build();

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(comprador));
        when(anuncioRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        assertThatThrownBy(() -> service.criar(dtoExcesso))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade solicitada maior que a disponível no anúncio");
    }

    @Test
    void deveLancarExcecaoAoConfirmarPedidoNaoPendente() {
        pedido.setStatus(StatusPedido.CONFIRMADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.confirmar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas pedidos pendentes podem ser confirmados");
    }

    @Test
    void deveLancarExcecaoAoFinalizarPedidoNaoConfirmado() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.finalizar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Apenas pedidos confirmados podem ser finalizados");
    }

    @Test
    void deveLancarExcecaoAoBuscarPedidoInexistente() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Pedido não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoAoListarPedidosDeCompradorInexistente() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorComprador(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }
}