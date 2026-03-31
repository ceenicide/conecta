package com.feira.conecta.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.PedidoDTO;
import com.feira.conecta.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pedidos", description = "Gerenciamento de pedidos entre compradores e vendedores")
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @Operation(summary = "Criar pedido", description = "Apenas usuários do tipo COMPRADOR podem fazer pedidos")
    @PostMapping
    public ResponseEntity<PedidoDTO> criar(@RequestBody @Valid PedidoDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @Operation(summary = "Buscar pedido por ID")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar pedidos por comprador")
    @GetMapping("/comprador/{compradorId}")
    public ResponseEntity<List<PedidoDTO>> listarPorComprador(@PathVariable Long compradorId) {
        return ResponseEntity.ok(service.listarPorComprador(compradorId));
    }

    @Operation(summary = "Confirmar pedido", description = "Confirma o pedido e marca o anúncio como VENDIDO")
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PedidoDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmar(id));
    }

    @Operation(summary = "Finalizar pedido", description = "Finaliza um pedido já confirmado")
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<PedidoDTO> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizar(id));
    }
}