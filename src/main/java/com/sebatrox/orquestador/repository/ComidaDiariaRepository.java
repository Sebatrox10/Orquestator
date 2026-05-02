package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.ComidaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComidaDiariaRepository extends JpaRepository<ComidaDiaria, Long> {
    // Esencial para calcular el total de calorías y macros de un día específico
    List<ComidaDiaria> findByPerfilUsuarioIdAndFechaHoraBetween(Long perfilId, LocalDateTime inicioDelDia, LocalDateTime finDelDia);
}
