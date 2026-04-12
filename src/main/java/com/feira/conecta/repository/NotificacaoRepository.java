package com.feira.conecta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.feira.conecta.domain.Notificacao;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuarioIdAndLidaFalseOrderByDataCriacaoDesc(Long usuarioId);

    List<Notificacao> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.usuario.id = :usuarioId AND n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId);
}