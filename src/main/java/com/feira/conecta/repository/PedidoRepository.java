package com.feira.conecta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.feira.conecta.domain.Pedido;
import com.feira.conecta.domain.StatusPedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByCompradorId(Long compradorId);
    List<Pedido> findByAnuncioId(Long anuncioId);
    List<Pedido> findByStatus(StatusPedido status);
}