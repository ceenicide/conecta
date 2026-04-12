package com.feira.conecta.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.feira.conecta.domain.Usuario;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;

    /**
     * Retorna o usuário autenticado a partir do token JWT.
     * Elimina IDOR: nunca confia no ID vindo do body.
     */
    public Usuario getUsuarioLogado() {
        String telefone = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return usuarioRepository.findByTelefone(telefone)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }
}
