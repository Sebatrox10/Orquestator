package com.sebatrox.orquestador.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sebatrox.orquestador.service.OrquestadorService;

@RestController
@RequestMapping("/api/automation")
public class AutomationController {

    @Autowired
    private OrquestadorService orquestadorService;

    // n8n enviará el PDF aquí

    
    @PostMapping(value = "/procesar-documento", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> recibirDesdeN8n(
            @RequestParam("chatId") long chatId,
            @RequestParam("nombreDocumento") String nombreDocumento,
            @RequestParam("archivo") MultipartFile archivo) {

        new Thread(() -> {
            try {
                // CORRECCIÓN: Agregamos .getBytes() para pasar los datos puros
                orquestadorService.procesarDocumentoPdfDirecto(chatId, archivo.getBytes(), nombreDocumento);
            } catch (Exception e) {
                System.err.println("Error procesando archivo: " + e.getMessage());
            }
        }).start();

        return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Procesamiento iniciado"));
    }

    // n8n enviará las preguntas de chat aquí
    @PostMapping("/preguntar")
    public ResponseEntity<Map<String, String>> preguntarDesdeN8n(@RequestBody Map<String, String> body) {
        String respuesta = orquestadorService.buscarContextoParaPregunta(body.get("pregunta"));
        // Al devolver un Map, Spring lo convierte automáticamente a JSON: {"data": "..."}
        return ResponseEntity.ok(Map.of("data", respuesta));
    }
}
