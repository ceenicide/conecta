package com.feira.conecta.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByTelefone(String telefone);
    List<Usuario> findByTipo(TipoUsuario tipo);
    boolean existsByTelefone(String telefone);
}