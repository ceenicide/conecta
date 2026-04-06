package com.feira.conecta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ofertas_futuras")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfertaFutura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private BigDecimal quantidade;

    @Column(nullable = false)
    private LocalDate dataDisponivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOferta status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = StatusOferta.ABERTA;
    }
}
