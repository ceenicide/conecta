package com.feira.conecta.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Notificacao;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.NotificacaoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.NotificacaoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final SecurityUtils securityUtils;

    /**
     * Cria e persiste uma notificação para um usuário específico.
     * Chamado pelo AnuncioMatchingService após encontrar um match.
     */
    @Transactional
    public void criar(Usuario usuario, String mensagem) {
        Notificacao notificacao = Notificacao.builder()
                .usuario(usuario)
                .mensagem(mensagem)
                .lida(false)
                .build();

        notificacaoRepository.save(notificacao);
        log.info("Notificação criada para usuário {} (id={}): {}", usuario.getNome(), usuario.getId(), mensagem);
    }

    /**
     * Retorna todas as notificações não lidas do usuário autenticado.
     */
    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listarNaoLidas() {
        Usuario usuario = securityUtils.getUsuarioLogado();
        return notificacaoRepository
                .findByUsuarioIdAndLidaFalseOrderByDataCriacaoDesc(usuario.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Retorna todas as notificações (lidas e não lidas) do usuário autenticado.
     */
    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listarTodas() {
        Usuario usuario = securityUtils.getUsuarioLogado();
        return notificacaoRepository
                .findByUsuarioIdOrderByDataCriacaoDesc(usuario.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Conta quantas notificações não lidas o usuário autenticado possui.
     */
    @Transactional(readOnly = true)
    public long contarNaoLidas() {
        Usuario usuario = securityUtils.getUsuarioLogado();
        return notificacaoRepository.countByUsuarioIdAndLidaFalse(usuario.getId());
    }

    /**
     * Marca uma notificação específica como lida.
     * Garante que o usuário só pode marcar suas próprias notificações.
     */
    @Transactional
    public NotificacaoDTO marcarComoLida(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada com id: " + id));

        if (!notificacao.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para acessar esta notificação");
        }

        notificacao.setLida(true);
        return toDTO(notificacaoRepository.save(notificacao));
    }

    /**
     * Marca todas as notificações do usuário autenticado como lidas.
     */
    @Transactional
    public int marcarTodasComoLidas() {
        Usuario usuario = securityUtils.getUsuarioLogado();
        return notificacaoRepository.marcarTodasComoLidas(usuario.getId());
    }

    private NotificacaoDTO toDTO(Notificacao n) {
        return NotificacaoDTO.builder()
                .id(n.getId())
                .usuarioId(n.getUsuario().getId())
                .mensagem(n.getMensagem())
                .lida(n.isLida())
                .dataCriacao(n.getDataCriacao())
                .build();
    }
}