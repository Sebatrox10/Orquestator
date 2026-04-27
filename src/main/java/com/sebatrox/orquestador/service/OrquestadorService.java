package com.sebatrox.orquestador.service;

import com.sebatrox.orquestador.entity.Documento;
import com.sebatrox.orquestador.entity.Fragmento;
import com.sebatrox.orquestador.repository.DocumentoRepository;
import com.sebatrox.orquestador.repository.FragmentoRepository;
import com.pgvector.PGvector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrquestadorService {

    @Autowired
    private FragmentoRepository fragmentoRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${OBSIDIAN_PATH}")
    private String baseObsidianPath;

    @Value("${agente.ia.url}/vectorizar")
    private String vectorizarUrl;

    @Value("${agente.ia.url}/generar-respuesta")
    private String generarRespuestaUrl;

    @Value("${agente.ia.url}/extraer-pdf")
    private String extraerPdfUrl;

    @Value("${agente.ia.url}/investigar-arxiv")
    private String investigarArxivUrl;

    @Value("${agente.ia.url}/editar-documento")
    private String editarDocumentoUrl;

    // 1. Memoria temporal para recordar las últimas búsquedas por chat
    private Map<Long, List<Map<String, String>>> cacheBusquedas = new ConcurrentHashMap<>();

    // Herramienta nativa de Spring para hacer llamadas HTTP a otros microservicios
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Esta será la dirección de tu microservicio en Python (lo construiremos en FastAPI)
    private final String PYTHON_WORKER_URL = vectorizarUrl; 


    private void enviarNotificacion(long chatId, String mensaje) {
        try {
            Map<String, Object> body = Map.of(
                "chatId", chatId,
                "mensaje", mensaje
            );
            // Reemplaza esto con la URL exacta de tu nodo Webhook en n8n
            restTemplate.postForEntity("http://100.99.94.122:5678/webhook/notificar-usuario", body, String.class);
        } catch (Exception e) {
            System.err.println("No se pudo enviar la notificación a n8n: " + e.getMessage());
        }
    }

    /**
     * Este es el método central. Recibe tu pregunta de Telegram y devuelve el contexto exacto.
     */
    public boolean procesarDocumentoPdfDirecto(long chatId, byte[] pdfBytes, String nombreDocumento) {
        return ejecutarLogicaProcesamiento(chatId, pdfBytes, nombreDocumento);
    }

    public String buscarContextoParaPregunta(String preguntaDelUsuario) {
        try {
            // 1. Pedirle a Python que vectorice la PREGUNTA
            Map<String, String> request = Map.of("texto", preguntaDelUsuario);
            @SuppressWarnings("unchecked")
            Map<String, List<Double>> response = restTemplate.postForObject(PYTHON_WORKER_URL, request, Map.class);

            List<Double> vectorList = response.get("vector");

            // 2. CONVERSIÓN VITAL: Pasar de List<Double> a float[] (Tu fix)
            float[] floatVector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                floatVector[i] = vectorList.get(i).floatValue();
            }

            // 3. Buscar en Postgres usando tu nuevo tipo de dato
            List<Fragmento> similares = fragmentoRepository.buscarSimilares(floatVector, 3);

            if (similares.isEmpty()) {
                return "No encontré información relevante en tus documentos guardados.";
            }

            // 1. Juntamos los trozos en un solo String de contexto
            StringBuilder contextoBuscado = new StringBuilder();
            for (Fragmento f : similares) {
                contextoBuscado.append(f.getContenido()).append("\n---\n");
            }

            // 2. LLAMADA A LA INTELIGENCIA (Python)
            Map<String, String> sintesisRequest = Map.of(
                "pregunta", preguntaDelUsuario,
                "contexto", contextoBuscado.toString()
            );

            @SuppressWarnings("unchecked")
            Map<String, String> sintesisResponse = restTemplate.postForObject(
                generarRespuestaUrl, sintesisRequest, Map.class);

            return sintesisResponse.get("respuesta");

        } catch (Exception e) {
            return "❌ Error en el cerebro de Troxi: " + e.getMessage();
        }
    }

    private List<String> fragmentarTexto(String texto) {
        List<String> fragmentos = new ArrayList<>();
        int tamanoChunk = 1000;
        int solapamiento = 100;
        int inicio = 0;

        while (inicio < texto.length()) {
            int fin = Math.min(inicio + tamanoChunk, texto.length());
            fragmentos.add(texto.substring(inicio, fin));
            
            // El nuevo inicio retrocede un poco para crear el solapamiento
            inicio += (tamanoChunk - solapamiento);
            
            // Evitar bucles infinitos si el texto es muy corto
            if (inicio >= texto.length() || tamanoChunk <= solapamiento) break;
        }
        return fragmentos;
    }

    public boolean procesarDocumentoPdf(long chatId, String fileId, String nombreDocumento) {
        try {
            enviarNotificacion(chatId, "⏳ Descargando PDF de Telegram: " + nombreDocumento);
            
            String getFileUrl = "https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + fileId;
            Map<String, Object> fileResponse = restTemplate.getForObject(getFileUrl, Map.class);
            Map<String, Object> result = (Map<String, Object>) fileResponse.get("result");
            String filePath = (String) result.get("file_path");
            String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;

            byte[] pdfBytes = restTemplate.getForObject(downloadUrl, byte[].class);
            
            return ejecutarLogicaProcesamiento(chatId, pdfBytes, nombreDocumento);
        } catch (Exception e) {
            enviarNotificacion(chatId, "❌ Error al descargar de Telegram: " + e.getMessage());
            return false;
        }
    }

    // --- EL CEREBRO DEL PROCESAMIENTO (Lógica Unificada) ---

    private boolean ejecutarLogicaProcesamiento(long chatId, byte[] pdfBytes, String nombreDocumento) {
        try {
            enviarNotificacion(chatId, "🧠 Analizando contenido con IA...");

            // 1. ENVIAR A PYTHON PARA EXTRAER TEXTO Y RESUMEN
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource contentsAsResource = new ByteArrayResource(pdfBytes) {
                @Override public String getFilename() { return nombreDocumento; }
            };
            body.add("file", contentsAsResource);

            @SuppressWarnings("unchecked")
            Map<String, String> extractionResponse = restTemplate.postForObject(
                extraerPdfUrl, body, Map.class);
            
            String textoCompleto = extractionResponse.get("texto");
            String temaCarpeta = extractionResponse.get("tema");
            String resumenInteligente = extractionResponse.get("resumen");

            enviarNotificacion(chatId, "📌 Tema detectado: **" + temaCarpeta + "**. Guardando en base de datos...");

            // 2. GUARDAR EN POSTGRES
            Documento doc = new Documento();
            doc.setTitulo(nombreDocumento);
            doc.setFechaProcesamiento(LocalDateTime.now());
            doc.setEstado("PROCESADO");
            doc = documentoRepository.save(doc);

            // 3. FRAGMENTAR Y VECTORIZAR
            List<String> trozos = fragmentarTexto(textoCompleto);
            for (String trozo : trozos) {
                Map<String, String> vectorRequest = Map.of("texto", trozo);
                @SuppressWarnings("unchecked")
                Map<String, List<Double>> vectorResponse = restTemplate.postForObject(
                    vectorizarUrl, vectorRequest, Map.class);

                List<Double> vectorList = vectorResponse.get("vector");
                float[] floatVector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) { floatVector[i] = vectorList.get(i).floatValue(); }

                Fragmento f = new Fragmento(); 
                f.setDocumento(doc); 
                f.setContenido(trozo); 
                f.setEmbedding(floatVector);
                fragmentoRepository.save(f);
            }

            // 4. CREAR NOTA EN OBSIDIAN
            String contenidoMd = "---\n" +
                "tags: [clase, universidad, pdf_procesado, " + temaCarpeta.toLowerCase() + "]\n" +
                "fecha: " + java.time.LocalDate.now() + "\n---\n" +
                "# " + nombreDocumento.replace(".pdf", "") + "\n\n" +
                "> [!success] Memoria RAM de Troxi Activa\n" +
                "> Documento indexado bajo la categoría **" + temaCarpeta + "**.\n\n" +
                "## 🧠 Resumen Inteligente\n" + resumenInteligente + "\n\n" +
                "---\n" +
                "## ✍️ Apuntes de Clase\n- [ ] \n";

            String rutaCarpetaAbsoluta = generarRutaCarpeta("Investigaciones/" + temaCarpeta);
            crearNotaEnObsidian(nombreDocumento.replace(".pdf", ""), contenidoMd, rutaCarpetaAbsoluta);

            enviarNotificacion(chatId, "✅ ¡Listo! La nota de **" + temaCarpeta + "** está en tu Obsidian.");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error crítico: " + e.getMessage());
            enviarNotificacion(chatId, "❌ Error procesando PDF: " + e.getMessage());
            return false;
        }
    }
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> investigarEnArxiv(long chatId, String tema) {
        try {
            enviarNotificacion(chatId, "🔎 Investigando en ArXiv sobre: " + tema + "...");

            Map<String, String> request = Map.of("texto", tema);
            Map<String, Object> response = restTemplate.postForObject(
                investigarArxivUrl, request, Map.class);
            
            if (response != null && response.containsKey("opciones")) {
                List<Map<String, String>> opciones = (List<Map<String, String>>) response.get("opciones");
                for (Map<String, String> opcion : opciones) {
                    opcion.put("tema_busqueda", tema);
                }
                cacheBusquedas.put(chatId, opciones); // <-- Guardamos la memoria
                return opciones;
            }
        } catch (Exception e) {
            System.err.println("❌ Error buscando en ArXiv: " + e.getMessage());
            enviarNotificacion(chatId, "❌ Error buscando en ArXiv.");
        }
        return new ArrayList<>();
    }

    // 3. El Método Maestro que crea la nota en Obsidian
    public void procesarSeleccionArxiv(long chatId, int indiceSeleccionado) {
        List<Map<String, String>> opciones = cacheBusquedas.get(chatId);
        if (opciones == null || indiceSeleccionado >= opciones.size()){
            enviarNotificacion(chatId, "❌ No encontré esa opción en mi memoria reciente.");
            return;
        }

        enviarNotificacion(chatId, "📥 Descargando e indexando el paper seleccionado...");

        Map<String, String> seleccion = opciones.get(indiceSeleccionado);
        String titulo = seleccion.get("titulo");
        String resumen = seleccion.get("resumen");
        String urlPdf = seleccion.get("url");


        // Recuperamos el tema y lo usamos para agrupar
        String temaBusqueda = seleccion.getOrDefault("tema_busqueda", "ArXiv").replace(" ", "_");
        String rutaCarpeta = generarRutaCarpeta("Investigaciones/ArXiv_" + temaBusqueda);
        String contenidoMd = armarPlantillaMarkdown(titulo, resumen, urlPdf);
        
        crearNotaEnObsidian(titulo, contenidoMd, rutaCarpeta);
        enviarNotificacion(chatId, "✅ Paper guardado en Obsidian: " + titulo);
    }

    // 4. Plantilla Zettelkasten para tu artículo científico
    private String armarPlantillaMarkdown(String titulo, String resumen, String url) {
        return "---\n" +
               "tags: [investigacion, agente, NLP]\n" +
               "fecha: " + java.time.LocalDate.now() + "\n" +
               "---\n" +
               "# " + titulo + "\n\n" +
               "**Enlace original (ArXiv):** [PDF Oficial](" + url + ")\n\n" +
               "## Resumen Inicial\n" +
               resumen + "\n\n" +
               "## Mentefacto / Notas\n" +
               "> [!NOTE] Siguientes Pasos\n" +
               "> - [ ] Leer el artículo completo en la tablet.\n" +
               "> - [ ] Extraer métricas clave para el background del paper.";
    }

    public String generarRutaCarpeta(String tema) {
        try {
            Path baseDir = Paths.get(baseObsidianPath, "Investigaciones", "Agente");
            Files.createDirectories(baseDir);

            // Contamos carpetas existentes para saber el número [00n]
            long numero = Files.list(baseDir).count() + 1;
            String carpetaNombre = String.format("[%03d] %s", numero, tema.replaceAll("[^a-zA-Z0-9]", "_"));
            
            Path rutaFinal = baseDir.resolve(carpetaNombre);
            Files.createDirectories(rutaFinal);
            
            return rutaFinal.toString(); // Retornamos la ruta completa para guardar la nota
        } catch (IOException e) {
            return baseObsidianPath; // Fallback
        }
    }

    public void crearNotaEnObsidian(String titulo, String contenidoMarkdown, String rutaCarpetaAbsoluta) {
        try {
            // 1. Usamos la ruta exacta que ya viene lista y comprobamos que exista
            Path pathCarpeta = Paths.get(rutaCarpetaAbsoluta);
            Files.createDirectories(pathCarpeta);

            // 2. Limpiamos el nombre del archivo
            String nombreLimpio = titulo.replaceAll("[^a-zA-Z0-9]", "_");
            
            // 3. RECORTAMOS el nombre a máximo 60 caracteres para evitar que Linux colapse
            if (nombreLimpio.length() > 60) {
                nombreLimpio = nombreLimpio.substring(0, 60);
            }
            
            String nombreArchivo = nombreLimpio + ".md";
            Path rutaFinal = pathCarpeta.resolve(nombreArchivo);

            // 4. Escribimos físicamente el archivo
            Files.writeString(rutaFinal, contenidoMarkdown);
            System.out.println("🗒️ Nota creada en Obsidian con éxito: " + rutaFinal);
            
        } catch (IOException e) {
            System.err.println("❌ Error al escribir en Obsidian: " + e.getMessage());
        }
    }

    public String editarNotaObsidian(String nombreArchivo, String instruccion) {
        try {
            // 1. Buscamos el archivo en toda la bóveda de Obsidian (recursivo)
            File carpetaBase = new File("/app/obsidian"); 
            File archivoEncontrado = buscarArchivoRecursivo(carpetaBase, nombreArchivo + ".md");
            
            if (archivoEncontrado == null) {
                return "❌ No encontré la nota '" + nombreArchivo + "' en ninguna carpeta de tu Obsidian.";
            }

            // 2. Leemos lo que tiene actualmente
            String contenidoActual = Files.readString(archivoEncontrado.toPath());

            // 3. Le pedimos a Python que procese la edición
            Map<String, String> request = Map.of(
                "contenido", contenidoActual,
                "instruccion", instruccion
            );
            
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.postForObject(
                editarDocumentoUrl, request, Map.class);
                
            String nuevoContenido = response.get("texto_editado");
            
            if ("ERROR".equals(nuevoContenido)) {
                return "❌ Troxi tuvo un problema al procesar la edición.";
            }

            // 4. Sobreescribimos el archivo con la versión mejorada
            Files.writeString(archivoEncontrado.toPath(), nuevoContenido, StandardOpenOption.TRUNCATE_EXISTING);
            
            return "✅ ¡Nota '" + nombreArchivo + "' actualizada con éxito!";

        } catch (Exception e) {
            return "❌ Error al acceder al archivo: " + e.getMessage();
        }
    }

    // Función para encontrar archivos dentro de subcarpetas dinámicas
    private File buscarArchivoRecursivo(File carpeta, String nombreArchivo) {
        File[] lista = carpeta.listFiles();
        if (lista != null) {
            for (File f : lista) {
                if (f.isDirectory()) {
                    File encontrado = buscarArchivoRecursivo(f, nombreArchivo);
                    if (encontrado != null) return encontrado;
                } else if (f.getName().equalsIgnoreCase(nombreArchivo)) {
                    return f;
                }
            }
        }
        return null;
    }
}
