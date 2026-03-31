// repository/MatchingRepository.java
package com.feira.conecta.repository;

import com.feira.conecta.domain.Matching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {
    List<Matching> findByOfertaId(Long ofertaId);
    List<Matching> findByDemandaId(Long demandaId);
    boolean existsByOfertaIdAndDemandaId(Long ofertaId, Long demandaId);
}