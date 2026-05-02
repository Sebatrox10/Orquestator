package com.sebatrox.orquestador.controller;

import com.sebatrox.orquestador.entity.ComidaDiaria;
import com.sebatrox.orquestador.entity.PerfilUsuario;
import com.sebatrox.orquestador.repository.PerfilUsuarioRepository;
import com.sebatrox.orquestador.service.NutricionService;
import com.sebatrox.orquestador.service.OrquestadorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nutricion")
public class NutritionController {

    @Autowired
    private NutricionService nutricionService;

    @Autowired
    private PerfilUsuarioRepository perfilRepository;

    @Autowired
    private OrquestadorService orquestadorService;

    // Endpoint para que n8n o Python registren una nueva comida
    @PostMapping("/registro-comida")
    public ResponseEntity<Map<String, String>> registrarComida(@RequestBody Map<String, Object> payload){
        try {
            // 1. Obtener o asignar perfil por defecto
            Long perfilId = payload.containsKey("perfilId") ? Long.valueOf(payload.get("perfilId").toString()) : 1L;
            
            PerfilUsuario perfil = perfilRepository.findById(perfilId)
                    .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

            // 2. Construir la entidad
            ComidaDiaria comida = new ComidaDiaria();
            comida.setPerfilUsuario(perfil);
            comida.setFechaHora(LocalDateTime.now());
            comida.setTipoComida((String) payload.get("tipoComida"));
            comida.setDescripcion((String) payload.get("descripcion"));
            
            // Parseo de macros numéricos de forma segura
            comida.setCalorias(Double.valueOf(payload.get("calorias").toString()));
            comida.setProteinas(Double.valueOf(payload.get("proteinas").toString()));
            comida.setCarbohidratos(Double.valueOf(payload.get("carbohidratos").toString()));
            comida.setGrasas(Double.valueOf(payload.get("grasas").toString()));

            if (payload.containsKey("urlImagen")) {
                comida.setUrlImagen((String) payload.get("urlImagen"));
            }

            // 3. Delegar el guardado (y futuras validaciones complejas) al servicio
            nutricionService.registrarComida(comida);

            return ResponseEntity.ok(Map.of("mensaje", "✅ Comida registrada exitosamente en la base de datos."));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ Error al registrar comida: " + e.getMessage()));
        }
    }

    // Endpoint para el Dashboard en React y consultas rápidas
    @GetMapping("/resumen-diario")
    public ResponseEntity<Map<String, Object>> obtenerResumenDiario(
            @RequestParam(defaultValue = "1") Long perfilId,
            @RequestParam(required = false) String fecha) {
        try {
            // 1. Determinar la fecha (hoy si no se especifica)
            LocalDate fechaConsulta = (fecha != null) ? LocalDate.parse(fecha) : LocalDate.now();

            // 2. Delegar la consulta al servicio
            List<ComidaDiaria> comidas = nutricionService.obtenerComidasDelDia(perfilId, fechaConsulta);

            // 3. Calcular totales para la respuesta del API
            double totalCalorias = 0, totalProteinas = 0, totalCarbos = 0, totalGrasas = 0;

            for (ComidaDiaria c : comidas) {
                totalCalorias += c.getCalorias() != null ? c.getCalorias() : 0;
                totalProteinas += c.getProteinas() != null ? c.getProteinas() : 0;
                totalCarbos += c.getCarbohidratos() != null ? c.getCarbohidratos() : 0;
                totalGrasas += c.getGrasas() != null ? c.getGrasas() : 0;
            }

            // 4. Formatear la respuesta (ideal para pintar gráficos en React)
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("fecha", fechaConsulta.toString());
            respuesta.put("totalComidas", comidas.size());
            respuesta.put("macros", Map.of(
                    "calorias", totalCalorias,
                    "proteinas", totalProteinas,
                    "carbohidratos", totalCarbos,
                    "grasas", totalGrasas
            ));
            respuesta.put("detalle", comidas);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al generar resumen: " + e.getMessage()));
        }
    }
    

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatInteligente(@RequestBody Map<String, Object> payload) {
        try {
            long chatId = Long.parseLong(payload.get("chatId").toString());
            String mensaje = (String) payload.get("mensaje");

            String respuesta = orquestadorService.procesarChatNutricion(chatId, mensaje);
            
            // Devolvemos un JSON para que n8n lo lea fácilmente
            return ResponseEntity.ok(Map.of("respuesta", respuesta));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // --- NUEVO: ENDPOINT PARA GENERAR DIETA ---
    @PostMapping("/generar-dieta")
    public ResponseEntity<Map<String, String>> generarDieta(@RequestBody Map<String, Object> payload) {
        try {
            long chatId = Long.parseLong(payload.get("chatId").toString());

            String dieta = orquestadorService.generarDietaDelDia(chatId);
            
            return ResponseEntity.ok(Map.of("respuesta", dieta));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // --- NUEVO: ENDPOINT PARA EXPORTACIÓN AUTOMÁTICA A OBSIDIAN ---
    @PostMapping("/exportar-bitacora")
    public ResponseEntity<Map<String, String>> exportarBitacora(@RequestBody Map<String, Object> payload) {
        try {
            // Como esto se ejecutará automáticamente, n8n deberá mandarnos el Chat ID
            long chatId = Long.parseLong(payload.get("chatId").toString());
            
            // Llamamos a la lógica maestra que redacta el Markdown
            String resultado = orquestadorService.exportarBitacoraNutricionalObsidian(chatId);
            
            return ResponseEntity.ok(Map.of("mensaje", resultado));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ Error al exportar a Obsidian: " + e.getMessage()));
        }
    }
}