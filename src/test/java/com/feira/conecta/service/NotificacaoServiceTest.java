package com.feira.conecta.service;

import java.time.LocalDateTime;
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
import com.feira.conecta.domain.Notificacao;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.NotificacaoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.NotificacaoRepository;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private NotificacaoService service;

    private Usuario vendedor;
    private Usuario comprador;
    private Notificacao notificacao;

    @BeforeEach
    void setup() {
        vendedor = Usuario.builder()
                .id(1L).nome("Maria").tipo(TipoUsuario.VENDEDOR).build();

        comprador = Usuario.builder()
                .id(2L).nome("Carlos").tipo(TipoUsuario.COMPRADOR).build();

        notificacao = Notificacao.builder()
                .id(1L)
                .usuario(vendedor)
                .mensagem("🎯 Match encontrado para Soja!")
                .lida(false)
                .dataCriacao(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------
    // criar()
    // -------------------------------------------------------

    @Test
    void deveCriarNotificacaoComSucesso() {
        when(notificacaoRepository.save(any())).thenReturn(notificacao);

        service.criar(vendedor, "🎯 Match encontrado para Soja!");

        verify(notificacaoRepository, times(1)).save(any(Notificacao.class));
    }

    @Test
    void deveCriarNotificacaoComLidaFalsePorPadrao() {
        when(notificacaoRepository.save(any())).thenAnswer(inv -> {
            Notificacao n = inv.getArgument(0);
            assertThat(n.isLida()).isFalse();
            return n;
        });

        service.criar(vendedor, "Qualquer mensagem");

        verify(notificacaoRepository, times(1)).save(any());
    }

    // -------------------------------------------------------
    // listarNaoLidas()
    // -------------------------------------------------------

    @Test
    void deveListarNotificacoesNaoLidasDoUsuarioLogado() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByDataCriacaoDesc(1L))
                .thenReturn(List.of(notificacao));

        List<NotificacaoDTO> resultado = service.listarNaoLidas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMensagem()).contains("Match");
        assertThat(resultado.get(0).isLida()).isFalse();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverNotificacoesNaoLidas() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByDataCriacaoDesc(1L))
                .thenReturn(List.of());

        assertThat(service.listarNaoLidas()).isEmpty();
    }

    // -------------------------------------------------------
    // listarTodas()
    // -------------------------------------------------------

    @Test
    void deveListarTodasNotificacoesDoUsuarioLogado() {
        Notificacao lida = Notificacao.builder()
                .id(2L).usuario(vendedor).mensagem("Outra msg").lida(true)
                .dataCriacao(LocalDateTime.now()).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(1L))
                .thenReturn(List.of(notificacao, lida));

        List<NotificacaoDTO> resultado = service.listarTodas();

        assertThat(resultado).hasSize(2);
    }

    // -------------------------------------------------------
    // contarNaoLidas()
    // -------------------------------------------------------

    @Test
    void deveContarNotificacoesNaoLidas() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.countByUsuarioIdAndLidaFalse(1L)).thenReturn(3L);

        long total = service.contarNaoLidas();

        assertThat(total).isEqualTo(3L);
    }

    // -------------------------------------------------------
    // marcarComoLida()
    // -------------------------------------------------------

    @Test
    void deveMarcarNotificacaoComoLida() {
        Notificacao lida = Notificacao.builder()
                .id(1L).usuario(vendedor).mensagem("msg").lida(true)
                .dataCriacao(LocalDateTime.now()).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));
        when(notificacaoRepository.save(any())).thenReturn(lida);

        NotificacaoDTO resultado = service.marcarComoLida(1L);

        assertThat(resultado.isLida()).isTrue();
        verify(notificacaoRepository, times(1)).save(any());
    }

    @Test
    void deveLancarExcecaoAoMarcarNotificacaoDeOutroUsuario() {
        when(securityUtils.getUsuarioLogado()).thenReturn(comprador);
        when(notificacaoRepository.findById(1L)).thenReturn(Optional.of(notificacao));

        assertThatThrownBy(() -> service.marcarComoLida(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Você não tem permissão para acessar esta notificação");
    }

    @Test
    void deveLancarExcecaoAoMarcarNotificacaoInexistente() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarComoLida(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notificação não encontrada com id: 99");
    }

    // -------------------------------------------------------
    // marcarTodasComoLidas()
    // -------------------------------------------------------

    @Test
    void deveMarcarTodasNotificacoesComoLidas() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.marcarTodasComoLidas(1L)).thenReturn(5);

        int atualizadas = service.marcarTodasComoLidas();

        assertThat(atualizadas).isEqualTo(5);
        verify(notificacaoRepository, times(1)).marcarTodasComoLidas(1L);
    }

    @Test
    void deveRetornarZeroQuandoNaoHouverNaoLidasParaMarcar() {
        when(securityUtils.getUsuarioLogado()).thenReturn(vendedor);
        when(notificacaoRepository.marcarTodasComoLidas(1L)).thenReturn(0);

        assertThat(service.marcarTodasComoLidas()).isZero();
    }
}