package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.MetaFitness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MetaFitnessRepository extends JpaRepository<MetaFitness, Long> {
    // Busca solo las metas que no hayas cumplido o abandonado
    List<MetaFitness> findByPerfilIdAndEstado(Long perfilId, String estado);
}
