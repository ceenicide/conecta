package com.feira.conecta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.config.JwtService;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.AuthDTO;
import com.feira.conecta.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Registro e login de usuários")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Operation(summary = "Registrar usuário", description = "Cria um novo usuário e retorna o token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Telefone já cadastrado ou dados inválidos")
    })
    @PostMapping("/registrar")
    public ResponseEntity<AuthDTO> registrar(@RequestBody @Valid AuthDTO dto) {
        if (usuarioRepository.existsByTelefone(dto.getTelefone())) {
            throw new IllegalArgumentException("Telefone já cadastrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .telefone(dto.getTelefone())
                .tipo(dto.getTipo())
                .build();

        usuarioRepository.save(usuario);
        String token = jwtService.gerarToken(dto.getTelefone());

        return ResponseEntity.ok(AuthDTO.builder()
                .telefone(dto.getTelefone())
                .nome(dto.getNome())
                .tipo(dto.getTipo())
                .token(token)
                .build());
    }

    @Operation(summary = "Login", description = "Autentica pelo telefone e retorna o token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Telefone não cadastrado")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthDTO> login(@RequestBody @Valid AuthDTO dto) {
        Usuario usuario = usuarioRepository.findByTelefone(dto.getTelefone())
                .orElseThrow(() -> new IllegalArgumentException("Telefone não cadastrado"));

        String token = jwtService.gerarToken(dto.getTelefone());

        return ResponseEntity.ok(AuthDTO.builder()
                .telefone(usuario.getTelefone())
                .nome(usuario.getNome())
                .tipo(usuario.getTipo())
                .token(token)
                .build());
    }
}