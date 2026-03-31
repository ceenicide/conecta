package com.feira.conecta.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.UsuarioDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;

    @Transactional
public UsuarioDTO criar(UsuarioDTO dto) {
    if (repository.existsByTelefone(dto.getTelefone())) {
        throw new IllegalArgumentException("Telefone já cadastrado: " + dto.getTelefone());
    }

    Usuario usuario = Usuario.builder()
            .nome(dto.getNome())
            .telefone(dto.getTelefone())
            .tipo(dto.getTipo())
            .createdAt(LocalDateTime.now()) // ← adiciona isso
            .build();

    return toDTO(repository.save(usuario));
}

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorId(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> buscarPorTipo(TipoUsuario tipo) {
        return repository.findByTipo(tipo).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));

        if (!usuario.getTelefone().equals(dto.getTelefone())
                && repository.existsByTelefone(dto.getTelefone())) {
            throw new IllegalArgumentException("Telefone já cadastrado: " + dto.getTelefone());
        }

        usuario.setNome(dto.getNome());
        usuario.setTelefone(dto.getTelefone());
        usuario.setTipo(dto.getTipo());

        return toDTO(repository.save(usuario));
    }

    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com id: " + id);
        }
        repository.deleteById(id);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .telefone(usuario.getTelefone())
                .tipo(usuario.getTipo())
                .createdAt(usuario.getCreatedAt())
                .build();
    }
}