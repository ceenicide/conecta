package com.feira.conecta.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.service.OfertaFuturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ofertas-futuras")
@RequiredArgsConstructor
@Tag(name = "Mercado futuro — Ofertas")
public class OfertaFuturaController {

    private final OfertaFuturaService service;

    @Operation(summary = "Criar oferta futura", description = "Apenas VENDEDOR. Usuário obtido do token JWT.")
    @SecurityRequirement(name = "Bearer")
    @PostMapping
    public ResponseEntity<OfertaFuturaDTO> criar(@RequestBody @Valid OfertaFuturaDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @Operation(summary = "Listar ofertas abertas")
    @GetMapping
    public ResponseEntity<List<OfertaFuturaDTO>> listarAbertas() {
        return ResponseEntity.ok(service.listarAbertas());
    }

    @Operation(summary = "Listar minhas ofertas")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/minhas")
    public ResponseEntity<List<OfertaFuturaDTO>> listarMinhasOfertas() {
        return ResponseEntity.ok(service.listarMinhasOfertas());
    }
}
