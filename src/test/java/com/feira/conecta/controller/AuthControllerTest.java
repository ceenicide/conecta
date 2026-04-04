package com.feira.conecta.controller;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.feira.conecta.config.JwtService;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.AuthDTO;
import com.feira.conecta.dto.UsuarioDTO;
import com.feira.conecta.repository.UsuarioRepository;
import com.feira.conecta.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthController controller;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder()
                .id(1L).nome("Maria Silva").telefone("11111111111")
                .tipo(TipoUsuario.VENDEDOR).build();
    }

    // CENÁRIOS FELIZES

    @Test
    void deveRegistrarUsuarioERetornarToken() {
        AuthDTO dto = AuthDTO.builder()
                .nome("Maria Silva").telefone("11111111111")
                .tipo(TipoUsuario.VENDEDOR).build();

        when(usuarioRepository.existsByTelefone("11111111111")).thenReturn(false);
        when(jwtService.gerarToken("11111111111")).thenReturn("token.gerado");

        ResponseEntity<AuthDTO> resposta = controller.registrar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getToken()).isEqualTo("token.gerado");
        assertThat(resposta.getBody().getNome()).isEqualTo("Maria Silva");
    }

    @Test
    void deveFazerLoginERetornarToken() {
        AuthDTO dto = AuthDTO.builder().telefone("11111111111").build();

        when(usuarioRepository.findByTelefone("11111111111"))
                .thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken("11111111111")).thenReturn("token.login");

        ResponseEntity<AuthDTO> resposta = controller.login(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getToken()).isEqualTo("token.login");
    }

    // CENÁRIOS INFELIZES

    @Test
    void deveLancarExcecaoQuandoTelefoneJaCadastradoNoRegistro() {
        AuthDTO dto = AuthDTO.builder()
                .nome("Maria").telefone("11111111111")
                .tipo(TipoUsuario.VENDEDOR).build();

        when(usuarioRepository.existsByTelefone("11111111111")).thenReturn(true);

        assertThatThrownBy(() -> controller.registrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telefone já cadastrado");
    }

    @Test
    void deveLancarExcecaoQuandoTelefoneNaoEncontradoNoLogin() {
        AuthDTO dto = AuthDTO.builder().telefone("99999999999").build();

        when(usuarioRepository.findByTelefone("99999999999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telefone não cadastrado");
    }
}