package com.feira.conecta.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.UsuarioDTO;
import com.feira.conecta.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock private UsuarioService service;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private UsuarioController controller;

    private Usuario usuarioLogado;
    private UsuarioDTO dto;

    @BeforeEach
    void setup() {
        usuarioLogado = Usuario.builder()
                .id(1L).nome("João Silva").telefone("(11) 999999999")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        dto = UsuarioDTO.builder()
                .id(1L).nome("João Silva").telefone("(11) 999999999")
                .tipo(TipoUsuario.VENDEDOR).build();
    }

    @Test
    void deveRetornarMeuPerfilERetornar200() {
        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado);
        when(service.buscarPorId(1L)).thenReturn(dto);

        ResponseEntity<UsuarioDTO> resposta = controller.meuPerfil();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getNome()).isEqualTo("João Silva");
    }

    @Test
    void deveRetornarDadosCorretosNoPerfil() {
        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado);
        when(service.buscarPorId(1L)).thenReturn(dto);

        ResponseEntity<UsuarioDTO> resposta = controller.meuPerfil();

        assertThat(resposta.getBody().getTelefone()).isEqualTo("(11) 999999999");
        assertThat(resposta.getBody().getTipo()).isEqualTo(TipoUsuario.VENDEDOR);
    }

    @Test
    void deveAtualizarMeuPerfilERetornar200() {
        UsuarioDTO atualizado = UsuarioDTO.builder()
                .id(1L).nome("João Atualizado").telefone("(11) 999999999")
                .tipo(TipoUsuario.COMPRADOR).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado);
        when(service.atualizar(eq(1L), any())).thenReturn(atualizado);

        ResponseEntity<UsuarioDTO> resposta = controller.atualizar(atualizado);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getNome()).isEqualTo("João Atualizado");
    }

    @Test
    void deveUsarIdDoTokenNaAtualizacao() {
        // Garante que o ID vem do token, não do body — proteção contra IDOR
        UsuarioDTO dtoComIdErrado = UsuarioDTO.builder()
                .id(99L) // ID diferente no body — deve ser ignorado
                .nome("João Atualizado").telefone("(11) 999999999")
                .tipo(TipoUsuario.VENDEDOR).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado); // retorna id=1
        when(service.atualizar(eq(1L), any())).thenReturn(dto); // verifica que usa id=1

        controller.atualizar(dtoComIdErrado);

        // Se chegou aqui sem erro, o service foi chamado com id=1 (do token), não 99 (do body)
        verify(service, times(1)).atualizar(eq(1L), any());
    }

    @Test
    void deveDeletarMinhaContaERetornar204() {
        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado);
        doNothing().when(service).deletar(1L);

        ResponseEntity<Void> resposta = controller.deletar();

        assertThat(resposta.getStatusCode().value()).isEqualTo(204);
        verify(service, times(1)).deletar(1L);
    }

    @Test
    void deveUsarIdDoTokenNaDelecao() {
        // Garante que apenas o próprio usuário pode deletar sua conta
        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado);
        doNothing().when(service).deletar(1L);

        controller.deletar();

        verify(securityUtils, times(1)).getUsuarioLogado();
        verify(service, times(1)).deletar(1L);
    }

    // REMOVIDO: deveListarTodosOsUsuariosERetornar200
    // Motivo: endpoint GET /usuarios foi removido por expor PII de todos os
    // usuários para qualquer pessoa autenticada. Sem caso de uso legítimo.
}