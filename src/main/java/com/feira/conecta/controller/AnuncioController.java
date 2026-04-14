package com.feira.conecta.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.AnuncioRequest;
import com.feira.conecta.dto.AnuncioResponse;
import com.feira.conecta.service.AnuncioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Anúncios", description = "Mercado imediato — anúncios de produtos disponíveis")
@RestController
@RequestMapping("/anuncios")
@RequiredArgsConstructor
public class AnuncioController {

    private final AnuncioService service;

    @Operation(summary = "Criar anúncio", description = "Apenas VENDEDOR. Usuário obtido do token JWT.")
    @SecurityRequirement(name = "Bearer")
    @PostMapping
    public ResponseEntity<AnuncioResponse> criar(@RequestBody @Valid AnuncioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Operation(summary = "Listar anúncios ativos")
    @GetMapping
    public ResponseEntity<List<AnuncioResponse>> listarAtivos() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @Operation(summary = "Buscar anúncio por ID")
    @GetMapping("/{id}")
    public ResponseEntity<AnuncioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar meus anúncios", description = "Retorna apenas os anúncios do usuário autenticado")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/meus")
    public ResponseEntity<List<AnuncioResponse>> listarMeusAnuncios() {
        return ResponseEntity.ok(service.listarMeusAnuncios());
    }

    @Operation(summary = "Marcar anúncio como vendido", description = "Apenas o dono do anúncio pode executar")
    @SecurityRequirement(name = "Bearer")
    @PatchMapping("/{id}/vendido")
    public ResponseEntity<AnuncioResponse> marcarComoVendido(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarComoVendido(id));
    }

    @Operation(summary = "Deletar anúncio", description = "Apenas o dono do anúncio pode executar")
    @SecurityRequirement(name = "Bearer")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}