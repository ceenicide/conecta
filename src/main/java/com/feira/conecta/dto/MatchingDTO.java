// dto/MatchingDTO.java
package com.feira.conecta.dto;

import java.time.LocalDateTime;

import com.feira.conecta.domain.StatusMatching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchingDTO {
    private Long id;
    private Long ofertaId;
    private String vendedorNome;
    private String produtoNome;
    private Long demandaId;
    private String compradorNome;
    private StatusMatching status;
    private LocalDateTime createdAt;
}