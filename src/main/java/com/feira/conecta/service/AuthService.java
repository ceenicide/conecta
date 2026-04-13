package com.feira.conecta.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.JwtService;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.auth.LoginRequest;
import com.feira.conecta.dto.auth.LoginResponse;
import com.feira.conecta.dto.auth.RegisterRequest;
import com.feira.conecta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * AuthService centraliza a lógica de autenticação que antes estava
 * diretamente no AuthController.
 *
 * PROBLEMA CORRIGIDO: AuthController acessava UsuarioRepository e JwtService
 * diretamente, violando a camada de Service. Controllers devem falar apenas
 * com Services — nunca com Repositories ou infraestrutura (JWT, crypto).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByTelefone(request.telefone())) {
            // Mensagem genérica — não revela se telefone existe (enumeração)
            throw new IllegalArgumentException("Não foi possível concluir o cadastro");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .telefone(request.telefone())
                .senha(passwordEncoder.encode(request.senha()))
                .tipo(request.tipo())
                .build();

        usuarioRepository.save(usuario);
        String token = jwtService.gerarToken(request.telefone());

        return new LoginResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTelefone(),
                usuario.getTipo(),
                token
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByTelefone(request.telefone())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        String token = jwtService.gerarToken(request.telefone());

        return new LoginResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTelefone(),
                usuario.getTipo(),
                token
        );
    }
}