package com.feira.conecta.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.PedidoRequest;
import com.feira.conecta.dto.PedidoResponse;
import com.feira.conecta.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pedidos", description = "Gerenciamento de pedidos")
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @Operation(summary = "Criar pedido", description = "Apenas COMPRADOR. Usuário obtido do token JWT.")
    @SecurityRequirement(name = "Bearer")
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @Operation(summary = "Buscar pedido por ID", description = "Acessível apenas pelo comprador ou vendedor envolvido")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar meus pedidos", description = "Retorna pedidos do comprador autenticado")
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/meus")
    public ResponseEntity<List<PedidoResponse>> listarMeusPedidos() {
        return ResponseEntity.ok(service.listarMeusPedidos());
    }

    @Operation(summary = "Confirmar pedido", description = "Apenas o VENDEDOR do anúncio pode confirmar")
    @SecurityRequirement(name = "Bearer")
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PedidoResponse> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmar(id));
    }

    @Operation(summary = "Finalizar pedido", description = "Apenas o COMPRADOR pode finalizar")
    @SecurityRequirement(name = "Bearer")
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<PedidoResponse> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizar(id));
    }
}