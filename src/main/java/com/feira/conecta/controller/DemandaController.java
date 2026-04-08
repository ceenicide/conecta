package com.feira.conecta.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.DemandaDTO;
import com.feira.conecta.dto.OfertaFuturaDTO;
import com.feira.conecta.service.DemandaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/demandas")
@RequiredArgsConstructor
@Tag(name = "Mercado futuro — Demandas")
public class DemandaController {

    private final DemandaService service;

    @Operation(summary = "Criar demanda", description = "Apenas COMPRADOR. Usuário obtido do token JWT.")
    @SecurityRequirement(name = "Bearer")
    @PostMapping
    public ResponseEntity<DemandaDTO> criar(@RequestBody @Valid DemandaDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @Operation(summary = "Listar demandas abertas")
    @GetMapping
    public ResponseEntity<List<DemandaDTO>> listarProcurando() {
        return ResponseEntity.ok(service.listarProcurando());
    }

    @Operation(summary = "Listar minhas demandas")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/minhas")
    public ResponseEntity<List<DemandaDTO>> listarMinhasDemandas() {
        return ResponseEntity.ok(service.listarMinhasDemandas());
    }

    /**
     * NOVO: retorna todas as ofertas futuras abertas cuja dataDisponivel
     * está dentro do prazo (dataLimite) da demanda informada.
     *
     * Exemplo de uso:
     *   GET /demandas/5/ofertas-compativeis
     *   → retorna ofertas com dataDisponivel <= demanda[5].dataLimite
     *
     * Protegido: apenas o comprador dono da demanda pode consultar.
     */
    @Operation(
        summary = "Ver ofertas compatíveis com minha demanda",
        description = "Retorna todas as ofertas abertas dentro do prazo da demanda. Apenas o dono da demanda pode consultar."
    )
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/{id}/ofertas-compativeis")
    public ResponseEntity<List<OfertaFuturaDTO>> listarOfertasCompativeis(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarOfertasCompativeis(id));
    }
}