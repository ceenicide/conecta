package com.feira.conecta.service;

import java.time.LocalDateTime;
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

import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.UsuarioDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private UsuarioService service;

    private Usuario usuario;
    private UsuarioDTO dto;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder()
                .id(1L)
                .nome("João Silva")
                .telefone("11999999999")
                .tipo(TipoUsuario.VENDEDOR)
                .createdAt(LocalDateTime.now())
                .build();

        dto = UsuarioDTO.builder()
                .nome("João Silva")
                .telefone("11999999999")
                .tipo(TipoUsuario.VENDEDOR)
                .build();
    }

    // CENÁRIOS FELIZES

    @Test
    void deveCriarUsuarioComSucesso() {
        when(repository.existsByTelefone(dto.getTelefone())).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenReturn(usuario);

        UsuarioDTO resultado = service.criar(dto);

        assertThat(resultado.getNome()).isEqualTo("João Silva");
        assertThat(resultado.getTipo()).isEqualTo(TipoUsuario.VENDEDOR);
        verify(repository, times(1)).save(any(Usuario.class));
    }

    @Test
    void deveListarTodosOsUsuarios() {
        when(repository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioDTO> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("João Silva");
    }

    @Test
    void deveBuscarUsuarioPorIdComSucesso() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioDTO resultado = service.buscarPorId(1L);

        assertThat(resultado.getTelefone()).isEqualTo("11999999999");
    }

    @Test
    void deveBuscarUsuariosPorTipo() {
        when(repository.findByTipo(TipoUsuario.VENDEDOR)).thenReturn(List.of(usuario));

        List<UsuarioDTO> resultado = service.buscarPorTipo(TipoUsuario.VENDEDOR);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTipo()).isEqualTo(TipoUsuario.VENDEDOR);
    }

    @Test
    void deveAtualizarUsuarioComSucesso() {
        UsuarioDTO dtoAtualizado = UsuarioDTO.builder()
                .nome("João Atualizado")
                .telefone("11999999999")
                .tipo(TipoUsuario.COMPRADOR)
                .build();

        Usuario usuarioAtualizado = Usuario.builder()
                .id(1L).nome("João Atualizado")
                .telefone("11999999999")
                .tipo(TipoUsuario.COMPRADOR)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.save(any(Usuario.class))).thenReturn(usuarioAtualizado);

        UsuarioDTO resultado = service.atualizar(1L, dtoAtualizado);

        assertThat(resultado.getNome()).isEqualTo("João Atualizado");
        assertThat(resultado.getTipo()).isEqualTo(TipoUsuario.COMPRADOR);
    }

    @Test
    void deveDeletarUsuarioComSucesso() {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        assertThatCode(() -> service.deletar(1L)).doesNotThrowAnyException();
        verify(repository, times(1)).deleteById(1L);
    }

    // CENÁRIOS INFELIZES

    @Test
    void deveLancarExcecaoQuandoTelefoneJaCadastrado() {
        when(repository.existsByTelefone(dto.getTelefone())).thenReturn(true);

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telefone já cadastrado: 11999999999");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }

    @Test
    void deveLancarExcecaoAoAtualizarComTelefoneDeOutroUsuario() {
        UsuarioDTO dtoComTelefoneNovo = UsuarioDTO.builder()
                .nome("João").telefone("11888888888").tipo(TipoUsuario.VENDEDOR).build();

        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.existsByTelefone("11888888888")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, dtoComTelefoneNovo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telefone já cadastrado: 11888888888");
    }

    @Test
    void deveLancarExcecaoAoDeletarUsuarioInexistente() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: 99");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverUsuarios() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listarTodos()).isEmpty();
    }
}