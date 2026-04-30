package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.RegistroEjercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroEjercicioRepository extends JpaRepository<RegistroEjercicio, Long> {
}
