package com.sebatrox.orquestador.controller;

import java.util.List;
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
    public ResponseEntity<Map<String, String>> preguntar(@RequestBody Map<String, String> body) {
        String pregunta = body.get("pregunta");
        // Extraemos el chatId del body que manda n8n
        long chatId = Long.parseLong(body.get("chatId").toString()); 

        String respuestaIA = orquestadorService.buscarContextoParaPregunta(pregunta, chatId);

        // DEVOLVEMOS UN JSON, NO UN STRING SOLO
        return ResponseEntity.ok(Map.of("respuesta", respuestaIA));

    }

    @PostMapping("/sincronizar-busqueda")
    public ResponseEntity<Void> sincronizar(@RequestBody Map<String, Object> payload) {
        long chatId = Long.parseLong(payload.get("chatId").toString());
        List<Map<String, String>> opciones = (List<Map<String, String>>) payload.get("opciones");
        
        // Guardamos en tu cache existente
        orquestadorService.getCacheBusquedas().put(chatId, opciones); 
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ejecutar-seleccion")
    public ResponseEntity<Void> ejecutar(@RequestBody Map<String, Object> body) {
        long chatId = Long.parseLong(body.get("chatId").toString());
        int index = Integer.parseInt(body.get("index").toString());
        
        orquestadorService.procesarSeleccionArxiv(chatId, index);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/editar-nota")
    public ResponseEntity<String> editar(@RequestBody Map<String, String> body) {
        String respuesta = orquestadorService.editarNotaObsidian(
            body.get("nombreArchivo"), 
            body.get("instruccion")
        );
        return ResponseEntity.ok(respuesta);
    }
}
