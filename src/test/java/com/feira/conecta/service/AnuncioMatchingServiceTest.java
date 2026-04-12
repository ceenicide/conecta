package com.feira.conecta.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Demanda;
import com.feira.conecta.domain.Produto;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.StatusDemanda;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.repository.DemandaRepository;

@ExtendWith(MockitoExtension.class)
class AnuncioMatchingServiceTest {

    @Mock private DemandaRepository demandaRepository;
    @Mock private NotificacaoService notificacaoService;

    @InjectMocks
    private AnuncioMatchingService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Produto produto;
    private Anuncio anuncio;
    private Demanda demanda;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").tipo(TipoUsuario.COMPRADOR).build();

        produto = Produto.builder()
                .id(1L).nome("Milho").usuario(vendedor).build();

        anuncio = Anuncio.builder()
                .id(10L).usuario(vendedor).produto(produto)
                .quantidade(new BigDecimal("200"))
                .preco(new BigDecimal("30.00"))
                .status(StatusAnuncio.ATIVO)
                .build();

        demanda = Demanda.builder()
                .id(20L).comprador(comprador).produto(produto)
                .quantidade(new BigDecimal("100"))
                .dataLimite(LocalDate.now().plusMonths(1))
                .status(StatusDemanda.PROCURANDO)
                .build();
    }

    @Test
    void deveGerarDuasNotificacoesQuandoHaUmMatch() {
        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of(demanda));

        service.executarMatching(anuncio);

        // Deve notificar vendedor E comprador — exatamente 2 chamadas
        verify(notificacaoService, times(2)).criar(any(Usuario.class), any(String.class));
    }

    @Test
    void deveNotificarVendedorComMensagemCorreta() {
        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of(demanda));

        service.executarMatching(anuncio);

        verify(notificacaoService, times(1)).criar(eq(vendedor), contains("Carlos"));
    }

    @Test
    void deveNotificarCompradorComMensagemCorreta() {
        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of(demanda));

        service.executarMatching(anuncio);

        verify(notificacaoService, times(1)).criar(eq(comprador), contains("Maria"));
    }

    @Test
    void naoDeveGerarNotificacaoQuandoNaoHouverDemandaCompativel() {
        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of());

        service.executarMatching(anuncio);

        verify(notificacaoService, never()).criar(any(), any());
    }

    @Test
    void deveGerarQuatroNotificacoesParaDoisMatches() {
        Demanda outraDemanda = Demanda.builder()
                .id(21L)
                .comprador(Usuario.builder().id(3L).nome("Ana").tipo(TipoUsuario.COMPRADOR).build())
                .produto(produto)
                .quantidade(new BigDecimal("50"))
                .dataLimite(LocalDate.now().plusMonths(2))
                .status(StatusDemanda.PROCURANDO)
                .build();

        when(demandaRepository.findByProdutoAndStatus(produto, StatusDemanda.PROCURANDO))
                .thenReturn(List.of(demanda, outraDemanda));

        service.executarMatching(anuncio);

        // 2 matches × 2 usuários = 4 notificações
        verify(notificacaoService, times(4)).criar(any(Usuario.class), any(String.class));
    }
}