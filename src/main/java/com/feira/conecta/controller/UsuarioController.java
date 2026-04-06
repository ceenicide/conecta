package com.feira.conecta.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.dto.UsuarioDTO;
import com.feira.conecta.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gerenciamento de usuários")
public class UsuarioController {

    private final UsuarioService service;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Meu perfil", description = "Retorna os dados do usuário autenticado")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> meuPerfil() {
        return ResponseEntity.ok(service.buscarPorId(securityUtils.getUsuarioLogado().getId()));
    }

    @Operation(summary = "Listar todos os usuários", description = "Requer autenticação")
    @SecurityRequirement(name = "Bearer")
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @Operation(summary = "Atualizar meu perfil", description = "Apenas o próprio usuário pode atualizar")
    @SecurityRequirement(name = "Bearer")
    @PutMapping("/me")
    public ResponseEntity<UsuarioDTO> atualizar(@RequestBody @Valid UsuarioDTO dto) {
        Long id = securityUtils.getUsuarioLogado().getId();
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @Operation(summary = "Deletar minha conta")
    @SecurityRequirement(name = "Bearer")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deletar() {
        service.deletar(securityUtils.getUsuarioLogado().getId());
        return ResponseEntity.noContent().build();
    }
}
