package com.sebatrox.orquestador.service;

import com.sebatrox.orquestador.entity.EjercicioTemplate;
import com.sebatrox.orquestador.entity.RutinaTemplate;
import com.sebatrox.orquestador.repository.RutinaTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sebatrox.orquestador.entity.SesionEntrenamiento;
import com.sebatrox.orquestador.entity.RegistroEjercicio;
import com.sebatrox.orquestador.repository.SesionEntrenamientoRepository;

@Service
public class FitnessService {

    @Autowired
    private RutinaTemplateRepository rutinaRepository;

    @Autowired
    private SesionEntrenamientoRepository sesionRepository;

    @Transactional
    public RutinaTemplate crearRutina(RutinaTemplate rutina) {
        // Asegurarnos de que cada ejercicio sepa a qué rutina pertenece antes de guardar
        if (rutina.getEjercicios() != null) {
            for (EjercicioTemplate ejercicio : rutina.getEjercicios()) {
                ejercicio.setRutinaTemplate(rutina);
            }
        }
        
        // Guardamos la rutina. Como pusimos CascadeType.ALL en la entidad, 
        // esto guardará automáticamente todos los ejercicios asociados.
        return rutinaRepository.save(rutina);
    }

    @Transactional
    public SesionEntrenamiento registrarSesion(SesionEntrenamiento sesion) {
        if (sesion.getEjerciciosRealizados() != null) {
            for (RegistroEjercicio registro : sesion.getEjerciciosRealizados()) {
                registro.setSesion(sesion);
            }
        }
        return sesionRepository.save(sesion);
    }
}
