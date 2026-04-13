package com.feira.conecta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.auth.LoginRequest;
import com.feira.conecta.dto.auth.LoginResponse;
import com.feira.conecta.dto.auth.RegisterRequest;
import com.feira.conecta.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AuthController refatorado.
 *
 * ANTES: acessava UsuarioRepository, JwtService e PasswordEncoder diretamente.
 * DEPOIS: delega tudo ao AuthService — controller faz apenas:
 *   1. Receber a requisição HTTP
 *   2. Chamar o service
 *   3. Montar o ResponseEntity com o status correto
 *
 * Status codes corrigidos:
 *   - Registro: 201 Created (antes era 200 OK)
 *   - Login: 200 OK (correto)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Registro e login de usuários")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário e retorna o token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou telefone já cadastrado")
    })
    @PostMapping("/registrar")
    public ResponseEntity<LoginResponse> registrar(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.registrar(request));
    }

    @Operation(summary = "Login", description = "Autentica pelo telefone e senha, retorna token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}