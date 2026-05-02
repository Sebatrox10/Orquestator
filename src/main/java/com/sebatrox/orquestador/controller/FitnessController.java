package com.sebatrox.orquestador.controller;

import com.sebatrox.orquestador.entity.RutinaTemplate;
import com.sebatrox.orquestador.service.FitnessService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.sebatrox.orquestador.service.OrquestadorService;

import com.sebatrox.orquestador.entity.SesionEntrenamiento;
import com.sebatrox.orquestador.repository.SesionEntrenamientoRepository;

@RestController
@RequestMapping("/api/fitness")
public class FitnessController {

    @Autowired
    private FitnessService fitnessService;

    @Autowired
    private OrquestadorService orquestadorService;

    @Autowired
    private SesionEntrenamientoRepository sesionRepository;

    @Autowired
    private com.sebatrox.orquestador.repository.RutinaTemplateRepository rutinaRepository;

    @GetMapping("/sesiones")
    public ResponseEntity<List<SesionEntrenamiento>> obtenerHistorial() {
        // Traemos las últimas 30 sesiones ordenadas por fecha (las más recientes al final para la gráfica)
        List<SesionEntrenamiento> historial = sesionRepository.findTop30ByOrderByIdAsc();
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/rutinas")
    public ResponseEntity<List<RutinaTemplate>> obtenerRutinasActivas() {
        // Traemos todas las rutinas (puedes ajustarlo luego para traer solo las de la semana actual)
        List<RutinaTemplate> rutinas = rutinaRepository.findAll();
        return ResponseEntity.ok(rutinas);
    }

    @DeleteMapping("/limpiar-datos")
    public ResponseEntity<Map<String, String>> limpiarBaseDeDatos() {
        try {
            // Borramos el historial y las planificaciones de prueba
            sesionRepository.deleteAll();
            rutinaRepository.deleteAll();
            return ResponseEntity.ok(Map.of("mensaje", "✅ Base de datos reseteada."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al borrar: " + e.getMessage()));
        }
    }

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

    @PostMapping(value = "/procesar-imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> procesarImagenDesdeN8n(@RequestParam("imagen") MultipartFile imagen) {
        try {
            System.out.println("📥 [JAVA] Recibiendo imagen desde n8n...");
            
            // Extraemos los datos crudos del archivo enviado por n8n
            byte[] imageBytes = imagen.getBytes();
            String fileName = imagen.getOriginalFilename();

            // Le pasamos la imagen al OrquestadorService para que se comunique con Python
            String jsonResult = orquestadorService.procesarImagenFitness(imageBytes, fileName);

            if (jsonResult == null) {
                return ResponseEntity.internalServerError().body("{\"error\": \"Falló el procesamiento en la IA de Python\"}");
            }

            // Devolvemos el JSON estructurado mágico de vuelta a n8n
            return ResponseEntity.ok(jsonResult);

        } catch (Exception e) {
            System.err.println("❌ [ERROR] Falló la recepción de la imagen en Java: " + e.getMessage());
            return ResponseEntity.internalServerError().body("{\"error\": \"Error interno en Java al leer la imagen\"}");
        }
    }

    @PostMapping(value = "/procesar-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> procesarAudioDesdeN8n(@RequestParam("audio") MultipartFile audio) {
        try {
            System.out.println("📥 [JAVA] Recibiendo nota de voz desde n8n...");
            byte[] audioBytes = audio.getBytes();
            String fileName = audio.getOriginalFilename();

            String jsonResult = orquestadorService.procesarAudioFitness(audioBytes, fileName);

            if (jsonResult == null) {
                return ResponseEntity.internalServerError().body("{\"error\": \"Falló el procesamiento de audio en la IA\"}");
            }

            return ResponseEntity.ok(jsonResult);

        } catch (Exception e) {
            System.err.println("❌ [ERROR] Falló la recepción del audio: " + e.getMessage());
            return ResponseEntity.internalServerError().body("{\"error\": \"Error interno al leer el audio\"}");
        }
    }

    @PostMapping("/consultar-coach")
    public ResponseEntity<Map<String, String>> consultarCoach(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("🧠 [JAVA] Recibiendo consulta para el Coach Inteligente...");
            
            // Extraemos los datos del JSON que enviará n8n
            long chatId = Long.parseLong(payload.get("chatId").toString());
            String mensajeSensacion = payload.get("mensaje").toString();

            // Llamamos a tu método maestro en el OrquestadorService
            String respuestaCoach = orquestadorService.consultarCoachInteligente(chatId, mensajeSensacion);

            // Spring Boot convertirá automáticamente este Map a un JSON: {"respuesta": "Lo que dijo Gemini..."}
            return ResponseEntity.ok(Map.of("respuesta", respuestaCoach));

        } catch (Exception e) {
            System.err.println("❌ [ERROR] Falló la consulta al Coach: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno al consultar la IA"));
        }
    }

    @PostMapping("/planificar")
    public ResponseEntity<Map<String, String>> planificarEntrenamiento(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("🗓️ [JAVA] Recibiendo solicitud de planificación semanal...");
            
            long chatId = Long.parseLong(payload.get("chatId").toString());
            String horarios = payload.get("horarios").toString();

            // Llamamos al Orquestador
            String respuestaCoach = orquestadorService.planificarSemana(chatId, horarios);

            return ResponseEntity.ok(Map.of("respuesta", respuestaCoach));

        } catch (Exception e) {
            System.err.println("❌ [ERROR] Falló el endpoint de planificación: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno al planificar"));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatLibre(@RequestBody Map<String, Object> payload) {
        long chatId = Long.parseLong(payload.get("chatId").toString());
        String mensaje = payload.get("mensaje").toString();
        
        String respuesta = orquestadorService.procesarChatInteligente(chatId, mensaje);
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }

    @PostMapping("/exportar")
    public ResponseEntity<Map<String, String>> exportarObsidian(@RequestBody Map<String, Object> payload) {
        long chatId = Long.parseLong(payload.get("chatId").toString());
        String respuesta = orquestadorService.exportarBitacoraObsidian(chatId);
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }

}
