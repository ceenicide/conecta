package com.feira.conecta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.feira.conecta.domain.Anuncio;
import com.feira.conecta.domain.StatusAnuncio;

@Repository
public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    List<Anuncio> findByStatus(StatusAnuncio status);
    List<Anuncio> findByUsuarioId(Long usuarioId);
    List<Anuncio> findByProdutoId(Long produtoId);
    List<Anuncio> findByUsuarioIdAndStatus(Long usuarioId, StatusAnuncio status);
}