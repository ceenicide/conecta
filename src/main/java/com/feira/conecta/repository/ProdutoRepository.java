package com.feira.conecta.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.feira.conecta.domain.Produto;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}