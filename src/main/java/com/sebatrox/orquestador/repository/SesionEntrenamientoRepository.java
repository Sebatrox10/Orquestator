package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.SesionEntrenamiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Long> {
    List<SesionEntrenamiento> findTop5ByOrderByFechaDesc();
    SesionEntrenamiento findFirstByOrderByIdDesc();
    List<SesionEntrenamiento> findTop30ByOrderByIdAsc();
}
