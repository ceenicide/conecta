package com.feira.conecta.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.feira.conecta.dto.NotificacaoDTO;
import com.feira.conecta.service.NotificacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notificações", description = "Alertas de match e eventos para o usuário autenticado")
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService service;

    @Operation(
            summary = "Listar notificações não lidas",
            description = "Retorna todas as notificações não lidas do usuário autenticado, ordenadas da mais recente para a mais antiga."
    )
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/nao-lidas")
    public ResponseEntity<List<NotificacaoDTO>> listarNaoLidas() {
        return ResponseEntity.ok(service.listarNaoLidas());
    }

    @Operation(
            summary = "Listar todas as notificações",
            description = "Retorna todas as notificações (lidas e não lidas) do usuário autenticado."
    )
    @SecurityRequirement(name = "Bearer")
    @GetMapping
    public ResponseEntity<List<NotificacaoDTO>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @Operation(
            summary = "Contagem de não lidas",
            description = "Retorna o total de notificações não lidas. Útil para exibir badges no frontend."
    )
    @SecurityRequirement(name = "Bearer")
    @GetMapping("/contagem")
    public ResponseEntity<Map<String, Long>> contarNaoLidas() {
        return ResponseEntity.ok(Map.of("naoLidas", service.contarNaoLidas()));
    }

    @Operation(
            summary = "Marcar notificação como lida",
            description = "Marca uma notificação específica como lida. O usuário só pode marcar suas próprias notificações."
    )
    @SecurityRequirement(name = "Bearer")
    @PatchMapping("/{id}/lida")
    public ResponseEntity<NotificacaoDTO> marcarComoLida(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarComoLida(id));
    }

    @Operation(
            summary = "Marcar todas como lidas",
            description = "Marca todas as notificações não lidas do usuário autenticado como lidas de uma vez."
    )
    @SecurityRequirement(name = "Bearer")
    @PatchMapping("/marcar-todas-lidas")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLidas() {
        int atualizadas = service.marcarTodasComoLidas();
        return ResponseEntity.ok(Map.of("notificacoesAtualizadas", atualizadas));
    }
}