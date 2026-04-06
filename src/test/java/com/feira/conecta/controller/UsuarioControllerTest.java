package com.feira.conecta.controller;

import java.util.List;

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
                .id(1L).nome("João Silva").telefone("11999999999")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        dto = UsuarioDTO.builder()
                .id(1L).nome("João Silva").telefone("11999999999")
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
    void deveListarTodosOsUsuariosERetornar200() {
        when(service.listarTodos()).thenReturn(List.of(dto));

        ResponseEntity<List<UsuarioDTO>> resposta = controller.listarTodos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveAtualizarMeuPerfilERetornar200() {
        UsuarioDTO atualizado = UsuarioDTO.builder()
                .id(1L).nome("João Atualizado").telefone("11999999999")
                .tipo(TipoUsuario.COMPRADOR).build();

        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado);
        when(service.atualizar(eq(1L), any())).thenReturn(atualizado);

        ResponseEntity<UsuarioDTO> resposta = controller.atualizar(atualizado);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getNome()).isEqualTo("João Atualizado");
    }

    @Test
    void deveDeletarMinhaContaERetornar204() {
        when(securityUtils.getUsuarioLogado()).thenReturn(usuarioLogado);
        doNothing().when(service).deletar(1L);

        ResponseEntity<Void> resposta = controller.deletar();

        assertThat(resposta.getStatusCode().value()).isEqualTo(204);
        verify(service, times(1)).deletar(1L);
    }
}
