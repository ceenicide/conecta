package com.feira.conecta.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.Pedido;
import com.feira.conecta.domain.StatusAnuncio;
import com.feira.conecta.domain.StatusPedido;
import com.feira.conecta.domain.TipoUsuario;
import com.feira.conecta.domain.Usuario;
import com.feira.conecta.dto.PedidoDTO;
import com.feira.conecta.exception.ResourceNotFoundException;
import com.feira.conecta.repository.AnuncioRepository;
import com.feira.conecta.repository.PedidoRepository;
import com.feira.conecta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnuncioRepository anuncioRepository;

    @Transactional
    public PedidoDTO criar(PedidoDTO dto) {
        Usuario comprador = usuarioRepository.findById(dto.getCompradorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado com id: " + dto.getCompradorId()));

        // só COMPRADOR pode fazer pedido
        if (comprador.getTipo() != TipoUsuario.COMPRADOR) {
            throw new IllegalArgumentException("Apenas compradores podem fazer pedidos");
        }

        Anuncio anuncio = anuncioRepository.findById(dto.getAnuncioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + dto.getAnuncioId()));

        // anúncio precisa estar ativo
        if (anuncio.getStatus() != StatusAnuncio.ATIVO) {
            throw new IllegalArgumentException("Este anúncio não está disponível");
        }

        // comprador não pode pedir do próprio anúncio
        if (anuncio.getUsuario().getId().equals(comprador.getId())) {
            throw new IllegalArgumentException("Você não pode fazer pedido do seu próprio anúncio");
        }

        // quantidade não pode ultrapassar o disponível
        if (dto.getQuantidade().compareTo(anuncio.getQuantidade()) > 0) {
            throw new IllegalArgumentException(
                    "Quantidade solicitada maior que a disponível no anúncio");
        }

        Pedido pedido = Pedido.builder()
        .comprador(comprador)
        .anuncio(anuncio)
        .quantidade(dto.getQuantidade())
        .status(StatusPedido.PENDENTE)
        .createdAt(LocalDateTime.now()) // ← adiciona isso
        .build();

        return toDTO(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public PedidoDTO buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPorComprador(Long compradorId) {
        if (!usuarioRepository.existsById(compradorId)) {
            throw new ResourceNotFoundException(
                    "Usuário não encontrado com id: " + compradorId);
        }
        return pedidoRepository.findByCompradorId(compradorId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public PedidoDTO confirmar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new IllegalArgumentException(
                    "Apenas pedidos pendentes podem ser confirmados");
        }

        // confirma o pedido e marca o anúncio como vendido
        pedido.setStatus(StatusPedido.CONFIRMADO);
        pedido.getAnuncio().setStatus(StatusAnuncio.VENDIDO);

        anuncioRepository.save(pedido.getAnuncio());
        return toDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoDTO finalizar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new IllegalArgumentException(
                    "Apenas pedidos confirmados podem ser finalizados");
        }

        pedido.setStatus(StatusPedido.FINALIZADO);
        return toDTO(pedidoRepository.save(pedido));
    }

    private PedidoDTO toDTO(Pedido pedido) {
        return PedidoDTO.builder()
                .id(pedido.getId())
                .compradorId(pedido.getComprador().getId())
                .compradorNome(pedido.getComprador().getNome())
                .anuncioId(pedido.getAnuncio().getId())
                .produtoNome(pedido.getAnuncio().getProduto().getNome())
                .vendedorNome(pedido.getAnuncio().getUsuario().getNome())
                .quantidade(pedido.getQuantidade())
                .status(pedido.getStatus())
                .createdAt(pedido.getCreatedAt())
                .build();
    }
}