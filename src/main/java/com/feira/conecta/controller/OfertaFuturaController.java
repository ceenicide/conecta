
package com.feira.conecta.controller;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.service.OfertaFuturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ofertas-futuras")
@RequiredArgsConstructor
@Tag(name = "Mercado futuro — Ofertas", description = "Ofertas de produtos que estarão disponíveis no futuro")
public class OfertaFuturaController {

    private final OfertaFuturaService service;

    @Operation(summary = "Criar oferta futura", description = "Apenas vendedores")
    @PostMapping
    public ResponseEntity<OfertaFuturaDTO> criar(@RequestBody @Valid OfertaFuturaDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @Operation(summary = "Listar ofertas abertas")
    @GetMapping
    public ResponseEntity<List<OfertaFuturaDTO>> listarAbertas() {
        return ResponseEntity.ok(service.listarAbertas());
    }

    @Operation(summary = "Listar ofertas por vendedor")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<OfertaFuturaDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.listarPorUsuario(usuarioId));
    }
}