package com.feira.conecta.controller;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.feira.conecta.config.JwtService;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.AuthDTO;
import com.feira.conecta.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController controller;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder()
                .id(1L).nome("Maria Silva").telefone("11111111111")
                .senha("$2a$10$hashbcrypt").tipo(TipoUsuario.VENDEDOR).build();
    }

    @Test
    void deveRegistrarUsuarioERetornarToken() {
        AuthDTO dto = AuthDTO.builder()
                .nome("Maria Silva").telefone("11111111111")
                .senha("senha123").tipo(TipoUsuario.VENDEDOR).build();

        when(usuarioRepository.existsByTelefone("11111111111")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hashbcrypt");
        when(jwtService.gerarToken("11111111111")).thenReturn("token.gerado");

        ResponseEntity<AuthDTO> resposta = controller.registrar(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getToken()).isEqualTo("token.gerado");
        assertThat(resposta.getBody().getNome()).isEqualTo("Maria Silva");
    }

    @Test
    void deveFazerLoginComSenhaCorretaERetornarToken() {
        AuthDTO dto = AuthDTO.builder()
                .telefone("11111111111").senha("senha123").build();

        when(usuarioRepository.findByTelefone("11111111111"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "$2a$10$hashbcrypt")).thenReturn(true);
        when(jwtService.gerarToken("11111111111")).thenReturn("token.login");

        ResponseEntity<AuthDTO> resposta = controller.login(dto);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().getToken()).isEqualTo("token.login");
    }

    @Test
    void deveLancarExcecaoQuandoTelefoneJaCadastradoNoRegistro() {
        AuthDTO dto = AuthDTO.builder()
                .nome("Maria").telefone("11111111111")
                .senha("senha123").tipo(TipoUsuario.VENDEDOR).build();

        when(usuarioRepository.existsByTelefone("11111111111")).thenReturn(true);

        // mensagem genérica — não revela que telefone já existe
        assertThatThrownBy(() -> controller.registrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Não foi possível concluir o cadastro");
    }

    @Test
    void deveLancarExcecaoQuandoTelefoneNaoEncontradoNoLogin() {
        AuthDTO dto = AuthDTO.builder()
                .telefone("99999999999").senha("senha123").build();

        when(usuarioRepository.findByTelefone("99999999999"))
                .thenReturn(Optional.empty());

        // mensagem genérica — não revela se telefone existe
        assertThatThrownBy(() -> controller.login(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciais inválidas");
    }

    @Test
    void deveLancarExcecaoQuandoSenhaIncorretaNoLogin() {
        AuthDTO dto = AuthDTO.builder()
                .telefone("11111111111").senha("senhaerrada").build();

        when(usuarioRepository.findByTelefone("11111111111"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaerrada", "$2a$10$hashbcrypt")).thenReturn(false);

        // mesma mensagem do telefone não encontrado — não revela qual campo está errado
        assertThatThrownBy(() -> controller.login(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciais inválidas");
    }

    @Test
    void deveLancarExcecaoQuandoRegistroSemSenha() {
        AuthDTO dto = AuthDTO.builder()
                .nome("Maria").telefone("11111111111")
                .tipo(TipoUsuario.VENDEDOR).build(); // sem senha

        when(usuarioRepository.existsByTelefone("11111111111")).thenReturn(false);

        assertThatThrownBy(() -> controller.registrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha é obrigatória no registro");
    }
}
