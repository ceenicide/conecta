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
import com.feira.conecta.dto.PedidoDTO;
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
    public PedidoDTO criar(PedidoDTO dto) {
        Usuario comprador = securityUtils.getUsuarioLogado();

        if (comprador.getTipo() != TipoUsuario.COMPRADOR) {
            throw new IllegalArgumentException("Apenas compradores podem fazer pedidos");
        }

        Anuncio anuncio = anuncioRepository.findById(dto.getAnuncioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anúncio não encontrado com id: " + dto.getAnuncioId()));

        if (anuncio.getStatus() != StatusAnuncio.ATIVO) {
            throw new IllegalArgumentException("Este anúncio não está disponível");
        }

        if (anuncio.getUsuario().getId().equals(comprador.getId())) {
            throw new IllegalArgumentException("Você não pode fazer pedido do seu próprio anúncio");
        }

        if (dto.getQuantidade().compareTo(anuncio.getQuantidade()) > 0) {
            throw new IllegalArgumentException("Quantidade solicitada maior que a disponível no anúncio");
        }

        Pedido pedido = Pedido.builder()
                .comprador(comprador)
                .anuncio(anuncio)
                .quantidade(dto.getQuantidade())
                .status(StatusPedido.PENDENTE)
                .createdAt(LocalDateTime.now())
                .build();

        return toDTO(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public PedidoDTO buscarPorId(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        // Só o comprador ou o vendedor do anúncio podem ver o pedido
        boolean isComprador = pedido.getComprador().getId().equals(usuario.getId());
        boolean isVendedor = pedido.getAnuncio().getUsuario().getId().equals(usuario.getId());

        if (!isComprador && !isVendedor) {
            throw new IllegalArgumentException("Você não tem permissão para visualizar este pedido");
        }

        return toDTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarMeusPedidos() {
        Usuario comprador = securityUtils.getUsuarioLogado();
        return pedidoRepository.findByCompradorId(comprador.getId()).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public PedidoDTO confirmar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        // Só o vendedor do anúncio pode confirmar
        if (!pedido.getAnuncio().getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para confirmar este pedido");
        }

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new IllegalArgumentException("Apenas pedidos pendentes podem ser confirmados");
        }

        pedido.setStatus(StatusPedido.CONFIRMADO);
        pedido.getAnuncio().setStatus(StatusAnuncio.VENDIDO);

        anuncioRepository.save(pedido.getAnuncio());
        return toDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoDTO finalizar(Long id) {
        Usuario usuario = securityUtils.getUsuarioLogado();

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado com id: " + id));

        // Só o comprador pode finalizar
        if (!pedido.getComprador().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para finalizar este pedido");
        }

        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new IllegalArgumentException("Apenas pedidos confirmados podem ser finalizados");
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
