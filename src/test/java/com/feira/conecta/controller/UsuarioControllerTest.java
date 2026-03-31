package com.feira.conecta.controller;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.dto.UsuarioDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService service;

    @InjectMocks
    private UsuarioController controller;

    private UsuarioDTO dto;

    @BeforeEach
    void setup() {
        dto = UsuarioDTO.builder()
                .id(1L)
                .nome("João Silva")
                .telefone("11999999999")
                .tipo(TipoUsuario.VENDEDOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========================
    // CENÁRIOS FELIZES
    // ========================

    @Test
    void deveCriarUsuarioERetornar200() {
        when(service.criar(any())).thenReturn(dto);

        ResponseEntity<UsuarioDTO> resposta = controller.criar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getNome()).isEqualTo("João Silva");
        assertThat(resposta.getBody().getTipo()).isEqualTo(TipoUsuario.VENDEDOR);
    }

    @Test
    void deveListarUsuariosERetornar200() {
        when(service.listarTodos()).thenReturn(List.of(dto));

        ResponseEntity<List<UsuarioDTO>> resposta = controller.listarTodos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).hasSize(1);
    }

    @Test
    void deveBuscarUsuarioPorIdERetornar200() {
        when(service.buscarPorId(1L)).thenReturn(dto);

        ResponseEntity<UsuarioDTO> resposta = controller.buscarPorId(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getTelefone()).isEqualTo("11999999999");
    }

    @Test
    void deveBuscarUsuariosPorTipoERetornar200() {
        when(service.buscarPorTipo(TipoUsuario.VENDEDOR)).thenReturn(List.of(dto));

        ResponseEntity<List<UsuarioDTO>> resposta = controller.buscarPorTipo(TipoUsuario.VENDEDOR);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().get(0).getTipo()).isEqualTo(TipoUsuario.VENDEDOR);
    }

    @Test
    void deveAtualizarUsuarioERetornar200() {
        UsuarioDTO dtoAtualizado = UsuarioDTO.builder()
                .id(1L).nome("João Atualizado")
                .telefone("11999999999")
                .tipo(TipoUsuario.COMPRADOR)
                .build();

        when(service.atualizar(eq(1L), any())).thenReturn(dtoAtualizado);

        ResponseEntity<UsuarioDTO> resposta = controller.atualizar(1L, dtoAtualizado);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getNome()).isEqualTo("João Atualizado");
        assertThat(resposta.getBody().getTipo()).isEqualTo(TipoUsuario.COMPRADOR);
    }

    @Test
    void deveDeletarUsuarioERetornar204() {
        doNothing().when(service).deletar(1L);

        ResponseEntity<Void> resposta = controller.deletar(1L);

        assertThat(resposta.getStatusCode().value()).isEqualTo(204);
        verify(service, times(1)).deletar(1L);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverUsuarios() {
        when(service.listarTodos()).thenReturn(List.of());

        ResponseEntity<List<UsuarioDTO>> resposta = controller.listarTodos();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isEmpty();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverUsuariosDotipoInformado() {
        when(service.buscarPorTipo(TipoUsuario.COMPRADOR)).thenReturn(List.of());

        ResponseEntity<List<UsuarioDTO>> resposta = controller.buscarPorTipo(TipoUsuario.COMPRADOR);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isEmpty();
    }

    // ========================
    // CENÁRIOS INFELIZES
    // ========================

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(service.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado com id: 99"));

        assertThatThrownBy(() -> controller.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoQuandoTelefoneJaCadastrado() {
        when(service.criar(any()))
                .thenThrow(new IllegalArgumentException("Telefone já cadastrado: 11999999999"));

        assertThatThrownBy(() -> controller.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telefone já cadastrado: 11999999999");
    }

    @Test
    void deveLancarExcecaoAoDeletarUsuarioInexistente() {
        doThrow(new ResourceNotFoundException("Usuário não encontrado com id: 99"))
                .when(service).deletar(99L);

        assertThatThrownBy(() -> controller.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }
}