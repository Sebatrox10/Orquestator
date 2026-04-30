package com.sebatrox.orquestador.controller;

import com.sebatrox.orquestador.entity.RutinaTemplate;
import com.sebatrox.orquestador.service.FitnessService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sebatrox.orquestador.entity.SesionEntrenamiento;

@RestController
@RequestMapping("/api/fitness")
public class FitnessController {

    @Autowired
    private FitnessService fitnessService;

    @PostMapping("/rutinas/template")
    public ResponseEntity<RutinaTemplate> crearRutinaTemplate(@RequestBody RutinaTemplate rutina) {
        // Recibe el JSON, se lo pasa al servicio para que asocie los ejercicios y lo guarda
        RutinaTemplate nuevaRutina = fitnessService.crearRutina(rutina);
        
        // Devuelve un código 200 OK y el objeto tal como quedó guardado en la BD (con sus IDs)
        return ResponseEntity.ok(nuevaRutina);
    }

    @PostMapping("/rutinas/sesion")
    public ResponseEntity<SesionEntrenamiento> registrarSesion(@RequestBody SesionEntrenamiento sesion) {
        SesionEntrenamiento nuevaSesion = fitnessService.registrarSesion(sesion);
        return ResponseEntity.ok(nuevaSesion);
    }
}
