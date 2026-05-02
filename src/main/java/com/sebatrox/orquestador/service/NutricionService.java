package com.sebatrox.orquestador.service;

import com.sebatrox.orquestador.entity.ComidaDiaria;
import com.sebatrox.orquestador.entity.ObjetivoNutricional;
import com.sebatrox.orquestador.repository.ComidaDiariaRepository;
import com.sebatrox.orquestador.repository.ObjetivoNutricionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class NutricionService {

    @Autowired
    private ComidaDiariaRepository comidaRepository;

    @Autowired
    private ObjetivoNutricionalRepository objetivoRepository;

    @Transactional
    public ComidaDiaria registrarComida(ComidaDiaria comida) {
        // Aquí a futuro puedes validar si superó las calorías del objetivo diario
        return comidaRepository.save(comida);
    }

    @Transactional(readOnly = true)
    public List<ComidaDiaria> obtenerComidasDelDia(Long perfilId, LocalDate fecha) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(LocalTime.MAX);
        return comidaRepository.findByPerfilUsuarioIdAndFechaHoraBetween(perfilId, inicioDia, finDia);
    }
    
    @Transactional(readOnly = true)
    public ObjetivoNutricional obtenerObjetivoActual(Long perfilId) {
        return objetivoRepository.findFirstByPerfilUsuarioIdAndEstadoOrderByFechaInicioDesc(perfilId, "ACTIVO")
                .orElse(null);
    }
}
