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
import com.feira.conecta.service.DemandaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/demandas")
@RequiredArgsConstructor
@Tag(name = "Mercado futuro — Demandas", description = "Demandas de produtos para datas futuras")
public class DemandaController {

    private final DemandaService service;

    @Operation(summary = "Criar demanda", description = "Apenas compradores")
    @PostMapping
    public ResponseEntity<DemandaDTO> criar(@RequestBody @Valid DemandaDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @Operation(summary = "Listar demandas procurando")
    @GetMapping
    public ResponseEntity<List<DemandaDTO>> listarProcurando() {
        return ResponseEntity.ok(service.listarProcurando());
    }

    @Operation(summary = "Listar demandas por comprador")
    @GetMapping("/comprador/{compradorId}")
    public ResponseEntity<List<DemandaDTO>> listarPorComprador(@PathVariable Long compradorId) {
        return ResponseEntity.ok(service.listarPorComprador(compradorId));
    }
}