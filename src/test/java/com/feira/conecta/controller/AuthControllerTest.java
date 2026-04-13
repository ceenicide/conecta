package com.feira.conecta.controller;

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

import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.dto.auth.LoginRequest;
import com.feira.conecta.dto.auth.LoginResponse;
import com.feira.conecta.dto.auth.RegisterRequest;
import com.feira.conecta.service.AuthService;

/**
 * Testes do AuthController após refatoração.
 *
 * MUDANÇAS em relação ao teste anterior:
 *
 * ANTES: controller injetava UsuarioRepository, JwtService e PasswordEncoder
 *   diretamente — os mocks refletiam isso.
 *
 * DEPOIS: controller delega tudo ao AuthService — o único mock necessário
 *   é AuthService. O teste do controller não precisa saber como o service
 *   implementa login/registro internamente: isso é responsabilidade dos
 *   testes do AuthService.
 *
 * Isso também corrige o status code: registro agora retorna 201 Created.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private LoginResponse loginResponse;
    private LoginResponse registerResponse;

    @BeforeEach
    void setup() {
        loginResponse = new LoginResponse(
                1L, "Maria Silva", "(11) 999999999",
                TipoUsuario.VENDEDOR, "token.login"
        );
        registerResponse = new LoginResponse(
                1L, "Maria Silva", "(11) 999999999",
                TipoUsuario.VENDEDOR, "token.registro"
        );
    }

    // ── Registro ────────────────────────────────────────────────────────────

    @Test
    void deveRegistrarUsuarioERetornar201() {
        RegisterRequest request = new RegisterRequest(
                "Maria Silva", "(11) 999999999", "senha123", TipoUsuario.VENDEDOR
        );
        when(authService.registrar(request)).thenReturn(registerResponse);

        ResponseEntity<LoginResponse> resposta = controller.registrar(request);

        // FIX: registro retorna 201 Created, não 200 OK
        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(resposta.getBody().token()).isEqualTo("token.registro");
        assertThat(resposta.getBody().nome()).isEqualTo("Maria Silva");
        assertThat(resposta.getBody().tipo()).isEqualTo(TipoUsuario.VENDEDOR);
    }

    @Test
    void deveRetornarIdETipoNoRegistro() {
        RegisterRequest request = new RegisterRequest(
                "Maria Silva", "(11) 999999999", "senha123", TipoUsuario.VENDEDOR
        );
        when(authService.registrar(request)).thenReturn(registerResponse);

        ResponseEntity<LoginResponse> resposta = controller.registrar(request);

        assertThat(resposta.getBody().id()).isEqualTo(1L);
        assertThat(resposta.getBody().tipo()).isEqualTo(TipoUsuario.VENDEDOR);
    }

    @Test
    void deveRepassarExcecaoDoServiceNoRegistro() {
        RegisterRequest request = new RegisterRequest(
                "Maria Silva", "(11) 999999999", "senha123", TipoUsuario.VENDEDOR
        );
        when(authService.registrar(request))
                .thenThrow(new IllegalArgumentException("Não foi possível concluir o cadastro"));

        assertThatThrownBy(() -> controller.registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Não foi possível concluir o cadastro");
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    void deveFazerLoginERetornar200() {
        LoginRequest request = new LoginRequest("(11) 999999999", "senha123");
        when(authService.login(request)).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> resposta = controller.login(request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody().token()).isEqualTo("token.login");
    }

    @Test
    void deveRetornarDadosDoUsuarioNoLogin() {
        LoginRequest request = new LoginRequest("(11) 999999999", "senha123");
        when(authService.login(request)).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> resposta = controller.login(request);

        assertThat(resposta.getBody().nome()).isEqualTo("Maria Silva");
        assertThat(resposta.getBody().telefone()).isEqualTo("(11) 999999999");
        assertThat(resposta.getBody().tipo()).isEqualTo(TipoUsuario.VENDEDOR);
    }

    @Test
    void deveRepassarExcecaoDeCredenciaisInvalidasDoService() {
        LoginRequest request = new LoginRequest("(99) 999999999", "senhaerrada");
        when(authService.login(request))
                .thenThrow(new IllegalArgumentException("Credenciais inválidas"));

        assertThatThrownBy(() -> controller.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciais inválidas");
    }
}