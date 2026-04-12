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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/matchings")
@RequiredArgsConstructor
@Tag(name = "Mercado futuro — Matching", description = "Correspondências entre ofertas e demandas")
public class MatchingController {

    private final MatchingService service;

    //  FIX: listarPorOferta agora exige autenticação — antes era público,
    //  expondo dados de negócio de qualquer oferta para qualquer pessoa.
    @Operation(summary = "Listar matches de uma oferta", description = "Apenas o dono da oferta pode ver seus matches")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/oferta/{ofertaId}")
    public ResponseEntity<List<MatchingDTO>> listarPorOferta(@PathVariable Long ofertaId) {
        return ResponseEntity.ok(service.listarPorOferta(ofertaId));
    }

    //  FIX: listarPorDemanda agora exige autenticação — idem ao anterior.
    @Operation(summary = "Listar matches de uma demanda", description = "Apenas o dono da demanda pode ver seus matches")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/demanda/{demandaId}")
    public ResponseEntity<List<MatchingDTO>> listarPorDemanda(@PathVariable Long demandaId) {
        return ResponseEntity.ok(service.listarPorDemanda(demandaId));
    }

    //  FIX: aceitar agora exige autenticação — antes PATCH /matchings/{id}/aceitar
    //         era acessível sem token (SecurityConfig só protegia /matchings/** via GET).
    //         Qualquer pessoa podia fechar ofertas e demandas de outros usuários.
    @Operation(summary = "Aceitar matching", description = "Apenas o VENDEDOR dono da oferta pode aceitar")
    @SecurityRequirement(name = "Bearer")
    @PatchMapping("/{id}/aceitar")
    public ResponseEntity<MatchingDTO> aceitar(@PathVariable Long id) {
        return ResponseEntity.ok(service.aceitar(id));
    }

    //  FIX: recusar agora exige autenticação — mesmo problema do aceitar.
    @Operation(summary = "Recusar matching", description = "Pode ser recusado pelo VENDEDOR ou COMPRADOR envolvido")
    @SecurityRequirement(name = "Bearer")
    @PatchMapping("/{id}/recusar")
    public ResponseEntity<MatchingDTO> recusar(@PathVariable Long id) {
        return ResponseEntity.ok(service.recusar(id));
    }
}