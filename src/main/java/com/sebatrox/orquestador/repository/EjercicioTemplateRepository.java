package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.EjercicioTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EjercicioTemplateRepository extends JpaRepository<EjercicioTemplate, Long> {
}