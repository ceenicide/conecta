package com.feira.conecta.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.feira.conecta.domain.Usuario;
import com.feira.conecta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String telefone) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByTelefone(telefone)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return User.builder()
                .username(usuario.getTelefone())
                .password(usuario.getSenha())
                .roles(usuario.getTipo().name())
                .build();
    }
}
