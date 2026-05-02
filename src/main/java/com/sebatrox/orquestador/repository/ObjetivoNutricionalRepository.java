package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.ObjetivoNutricional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ObjetivoNutricionalRepository extends JpaRepository<ObjetivoNutricional, Long> {
    Optional<ObjetivoNutricional> findFirstByPerfilUsuarioIdAndEstadoOrderByFechaInicioDesc(Long perfilId, String estado);
}
