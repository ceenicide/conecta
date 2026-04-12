package com.feira.conecta.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.feira.conecta.config.CustomUserDetailsService;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UsuarioRepository repository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void deveCarregarUsuarioPorTelefone() {
        Usuario usuario = Usuario.builder()
                .id(1L).nome("Maria").telefone("11111111111")
                .senha("$2a$10$hashbcrypt").tipo(TipoUsuario.VENDEDOR).build();

        when(repository.findByTelefone("11111111111")).thenReturn(Optional.of(usuario));

        UserDetails resultado = service.loadUserByUsername("11111111111");

        assertThat(resultado.getUsername()).isEqualTo("11111111111");
        // senha agora é o hash real, não vazio
        assertThat(resultado.getPassword()).isEqualTo("$2a$10$hashbcrypt");
        assertThat(resultado.getAuthorities()).isNotEmpty();
    }

    @Test
    void deveLancarExcecaoQuandoTelefoneNaoEncontrado() {
        when(repository.findByTelefone("99999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("99999999999"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void deveAtribuirRoleCorretaAoVendedor() {
        Usuario vendedor = Usuario.builder()
                .id(1L).nome("Maria").telefone("11111111111")
                .senha("hash").tipo(TipoUsuario.VENDEDOR).build();

        when(repository.findByTelefone("11111111111")).thenReturn(Optional.of(vendedor));

        UserDetails resultado = service.loadUserByUsername("11111111111");

        assertThat(resultado.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDEDOR"));
    }

    @Test
    void deveAtribuirRoleCorretaAoComprador() {
        Usuario comprador = Usuario.builder()
                .id(2L).nome("Carlos").telefone("22222222222")
                .senha("hash").tipo(TipoUsuario.COMPRADOR).build();

        when(repository.findByTelefone("22222222222")).thenReturn(Optional.of(comprador));

        UserDetails resultado = service.loadUserByUsername("22222222222");

        assertThat(resultado.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPRADOR"));
    }
}
