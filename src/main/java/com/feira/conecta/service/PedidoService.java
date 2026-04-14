package com.feira.conecta.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.config.SecurityUtils;
import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Pedido;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.StatusPedido;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.PedidoRequest;
import com.feira.conecta.dto.PedidoResponse;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.AnuncioRepository;
import com.feira.conecta.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final AnuncioRepository anuncioRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public PedidoResponse criar(PedidoRequest request) {
        Usuario comprador = securityUtils.getUsuarioLogado();

        if (comprador.getTipo() != TipoUsuario.COMPRADOR) {
            throw new IllegalArgumentException("Apenas compradores podem fazer pedidos");
        }

        Anuncio anuncio = anuncioRepository.findById(request.anuncioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + request.anuncioId()));

        if (anuncio.getStatus() != StatusAnuncio.ATIVO) {
            throw new IllegalArgumentException("Este anúncio não está disponível");
        }

        if (anuncio.getUsuario().getId().equals(comprador.getId())) {
            throw new IllegalArgumentException("Você não pode fazer pedido do seu próprio anúncio");
        }

        if (request.quantidade().compareTo(anuncio.getQuantidade()) > 0) {
            throw new IllegalArgumentException("Quantidade solicitada maior que a disponível no anúncio");
        }

        Pedido pedido = Pedido.builder()
                .comprador(comprador)
                .anuncio(anuncio)
                .quantidade(request.quantidade())
                .status(StatusPedido.PENDENTE)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        boolean isComprador = pedido.getComprador().getId().equals(usuario.getId());
        boolean isVendedor = pedido.getAnuncio().getUsuario().getId().equals(usuario.getId());

        if (!isComprador && !isVendedor) {
            throw new IllegalArgumentException("Você não tem permissão para visualizar este pedido");
        }

        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarMeusPedidos() {
        Usuario comprador = securityUtils.getUsuarioLogado();
        return pedidoRepository.findByCompradorId(comprador.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PedidoResponse confirmar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        if (!pedido.getAnuncio().getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para confirmar este pedido");
        }

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new IllegalArgumentException("Apenas pedidos pendentes podem ser confirmados");
        }

        pedido.setStatus(StatusPedido.CONFIRMADO);
        pedido.getAnuncio().setStatus(StatusAnuncio.VENDIDO);

        anuncioRepository.save(pedido.getAnuncio());
        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse finalizar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        if (!pedido.getComprador().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para finalizar este pedido");
        }

        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new IllegalArgumentException("Apenas pedidos confirmados podem ser finalizados");
        }

        pedido.setStatus(StatusPedido.FINALIZADO);
        return toResponse(pedidoRepository.save(pedido));
    }

    private PedidoResponse toResponse(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getComprador().getId(),
                pedido.getComprador().getNome(),
                pedido.getAnuncio().getId(),
                pedido.getAnuncio().getProduto().getNome(),
                pedido.getAnuncio().getUsuario().getNome(),
                pedido.getQuantidade(),
                pedido.getStatus(),
                pedido.getCreatedAt()
        );
    }
}