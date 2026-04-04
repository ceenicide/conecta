package com.feira.conecta.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.MatchingDTO;
import com.feira.conecta.service.MatchingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/matchings")
@RequiredArgsConstructor
@Tag(name = "Mercado futuro — Matching", description = "Correspondências entre ofertas e demandas")
public class MatchingController {

    private final MatchingService service;

    @Operation(summary = "Listar matches de uma oferta")
    @GetMapping("/oferta/{ofertaId}")
    public ResponseEntity<List<MatchingDTO>> listarPorOferta(@PathVariable Long ofertaId) {
        return ResponseEntity.ok(service.listarPorOferta(ofertaId));
    }

    @Operation(summary = "Listar matches de uma demanda")
    @GetMapping("/demanda/{demandaId}")
    public ResponseEntity<List<MatchingDTO>> listarPorDemanda(@PathVariable Long demandaId) {
        return ResponseEntity.ok(service.listarPorDemanda(demandaId));
    }

    @Operation(summary = "Aceitar matching", description = "Fecha a oferta e marca a demanda como atendida")
    @PatchMapping("/{id}/aceitar")
    public ResponseEntity<MatchingDTO> aceitar(@PathVariable Long id) {
        return ResponseEntity.ok(service.aceitar(id));
    }

    @Operation(summary = "Recusar matching")
    @PatchMapping("/{id}/recusar")
    public ResponseEntity<MatchingDTO> recusar(@PathVariable Long id) {
        return ResponseEntity.ok(service.recusar(id));
    }
}