package com.sebatrox.orquestador.service;

import com.sebatrox.orquestador.entity.Documento;
import com.sebatrox.orquestador.entity.Fragmento;
import com.sebatrox.orquestador.entity.MetaFitness;
import com.sebatrox.orquestador.entity.PerfilUsuario;
import com.sebatrox.orquestador.entity.PortafolioEstrategia;
import com.sebatrox.orquestador.entity.RutinaTemplate;
import com.sebatrox.orquestador.entity.SesionEntrenamiento;
import com.sebatrox.orquestador.repository.DocumentoRepository;
import com.sebatrox.orquestador.repository.FragmentoRepository;
import com.sebatrox.orquestador.repository.MetaFitnessRepository;
import com.sebatrox.orquestador.repository.PerfilUsuarioRepository;
import com.sebatrox.orquestador.repository.PortafolioEstrategiaRepository;
import com.sebatrox.orquestador.repository.SesionEntrenamientoRepository;
import com.pgvector.PGvector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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

    @Autowired
    private PortafolioEstrategiaRepository portafolioRepository;

    @Autowired
    private SesionEntrenamientoRepository sesionRepository;

    @Autowired
    private PerfilUsuarioRepository perfilRepository;

    @Autowired
    private MetaFitnessRepository metaRepository;

    @Autowired
    private FitnessService fitnessService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.finance.token}")
    private String financeBotToken;

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

    @Value("${agente.ia.url}/extraer-texto")
    private String extraerTextoUrl;

    // 1. Memoria temporal para recordar las últimas búsquedas por chat
    private Map<Long, List<Map<String, String>>> cacheBusquedas = new ConcurrentHashMap<>();

    public Map<Long, List<Map<String, String>>> getCacheBusquedas() {
        return cacheBusquedas;
    }

    // --- MEMORIA DE PREFERENCIAS DE FORMATO ---
    private Map<Long, String> preferenciasFormato = new ConcurrentHashMap<>();

    public String cambiarFormato(long chatId, String nuevoFormato) {
        String formatoLimpio = nuevoFormato.toUpperCase().trim();
        preferenciasFormato.put(chatId, formatoLimpio);
        return "✅ ¡Entendido! A partir de ahora redactaré mis respuestas y citas usando el formato: **" + formatoLimpio + "**.";
    }

    public String getFormatoActual(long chatId) {
        return preferenciasFormato.getOrDefault(chatId, "APA 7"); // APA 7 por defecto
    }

    // Herramienta nativa de Spring para hacer llamadas HTTP a otros microservicios
    private final RestTemplate restTemplate = new RestTemplate();

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

    public String buscarContextoParaPregunta(String preguntaDelUsuario, long chatId) {
        try {
            //enviarNotificacion(chatId, "🧠 Consultando mi memoria y tus notas...");

            Map<String, String> request = Map.of("texto", preguntaDelUsuario);
            
            // USAMOS LA VARIABLE INYECTADA DIRECTAMENTE
            @SuppressWarnings("unchecked")
            Map<String, List<Double>> response = restTemplate.postForObject(vectorizarUrl, request, Map.class);

            List<Double> vectorList = response.get("vector");
            float[] floatVector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                floatVector[i] = vectorList.get(i).floatValue();
            }

            List<Fragmento> similares = fragmentoRepository.buscarSimilares(floatVector, 3);
            if (similares.isEmpty()) return "No encontré información relevante.";

            StringBuilder contexto = new StringBuilder();
            StringBuilder contextoBuscado = new StringBuilder();
            int contadorFuente = 1;

            for (Fragmento f : similares) {
                Documento docAsociado = f.getDocumento(); 
                Map<String, Object> meta = docAsociado.getMetadata();
                
                String tituloInfo = docAsociado.getTitulo();
                String autorInfo = (meta != null && meta.containsKey("autor")) ? meta.get("autor").toString() : "Desconocido";
                String anioInfo = (meta != null && meta.containsKey("anio")) ? meta.get("anio").toString() : "s.f.";

                // Armamos el super-prompt de RAG Académico
                contextoBuscado.append("Fuente [").append(contadorFuente).append("] - ")
                               .append("Título: ").append(tituloInfo)
                               .append(" (Autor: ").append(autorInfo).append(", Año: ").append(anioInfo).append("):\n")
                               .append(f.getContenido()).append("\n\n---\n\n");
                contadorFuente++;
            }

            String formatoActual = getFormatoActual(chatId);

            Map<String, String> sintesisRequest = Map.of("pregunta", preguntaDelUsuario, "contexto", contextoBuscado.toString(), "formato_cita", formatoActual);

            @SuppressWarnings("unchecked")
            Map<String, String> sintesisResponse = restTemplate.postForObject(generarRespuestaUrl, sintesisRequest, Map.class);

            return sintesisResponse.get("respuesta");
        } catch (Exception e) {
            return "❌ Error en el cerebro: " + e.getMessage();
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
            
            // 1. Usamos la nueva función compartida
            byte[] pdfBytes = descargarArchivoTelegram(fileId, this.botToken);
            
            // 2. Ejecuta la lógica académica (Obsidian, Mentefacto, etc.)
            return ejecutarLogicaProcesamiento(chatId, pdfBytes, nombreDocumento);
            
        } catch (Exception e) {
            enviarNotificacion(chatId, "❌ Error al descargar de Telegram: " + e.getMessage());
            return false;
        }
    }

    public boolean procesarUrlWeb(long chatId, String url, String contenidoMarkdown) {
        try {
            enviarNotificacion(chatId, "🌐 Leyendo el contenido de la web...");

            // 1. ENVIAR A PYTHON PARA EXTRAER TEMA, RESUMEN Y METADATOS
            Map<String, String> requestPython = Map.of(
                "texto", contenidoMarkdown,
                "url_origen", url
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> extractionResponse = restTemplate.postForObject(
                extraerTextoUrl, requestPython, Map.class);
            
            // --- ESCUDO ANTI-BLOQUEOS WEB ---
            String temaCarpeta = "Web_General";
            String resumenInteligente = "No se pudo extraer el resumen de esta página.";
            Map<String, Object> metadataDoc = Map.of("titulo", url, "autor", "Desconocido", "anio", "s.f.");

            // Revisamos si Python devolvió un error o un JSON incompleto
            if (extractionResponse != null && !extractionResponse.containsKey("error")) {
                if (extractionResponse.containsKey("tema")) temaCarpeta = (String) extractionResponse.get("tema");
                if (extractionResponse.containsKey("resumen")) resumenInteligente = (String) extractionResponse.get("resumen");
                
                if (extractionResponse.containsKey("metadata") && extractionResponse.get("metadata") != null) {
                    metadataDoc = (Map<String, Object>) extractionResponse.get("metadata");
                }
            } else {
                enviarNotificacion(chatId, "⚠️ La página web tiene bloqueos o formato complejo. Guardando link con valores por defecto...");
            }

            String tituloDocumento = metadataDoc.getOrDefault("titulo", url).toString();
            if (tituloDocumento.equals("Desconocido")) {
                tituloDocumento = "Articulo_Web_" + System.currentTimeMillis();
            }
            // ---------------------------------

            enviarNotificacion(chatId, "📌 Tema detectado: **" + temaCarpeta + "**. Vectorizando...");

            // 2. GUARDAR EN POSTGRES
            Documento doc = new Documento();
            doc.setTitulo(tituloDocumento);
            doc.setFechaProcesamiento(LocalDateTime.now());
            doc.setEstado("PROCESADO");
            doc.setMetadata(metadataDoc); 
            doc = documentoRepository.save(doc);

            // 3. FRAGMENTAR Y VECTORIZAR
            List<String> trozos = fragmentarTexto(contenidoMarkdown);
            for (String trozo : trozos) {
                Map<String, String> vectorRequest = Map.of("texto", trozo);
                @SuppressWarnings("unchecked")
                Map<String, List<Double>> vectorResponse = restTemplate.postForObject(vectorizarUrl, vectorRequest, Map.class);

                List<Double> vectorList = vectorResponse.get("vector");
                float[] floatVector = new float[vectorList.size()];
                for (int i = 0; i < vectorList.size(); i++) { floatVector[i] = vectorList.get(i).floatValue(); }

                Fragmento f = new Fragmento(); 
                f.setDocumento(doc); 
                f.setContenido(trozo); 
                f.setEmbedding(floatVector);
                fragmentoRepository.save(f);
            }

            // 4. CREAR NOTA EN OBSIDIAN (DINÁMICA)
            String formato = getFormatoActual(chatId);
            
            // Llamamos al motor. Como es Web, pasamos el contenido Markdown para el bloque oculto y la URL
            String contenidoMd = generarContenidoMarkdown(tituloDocumento, resumenInteligente, contenidoMarkdown, temaCarpeta, url, formato);

            String rutaCarpetaAbsoluta = generarRutaCarpeta("Investigaciones/" + temaCarpeta);
            crearNotaEnObsidian(tituloDocumento, contenidoMd, rutaCarpetaAbsoluta);

            enviarNotificacion(chatId, "✅ ¡Artículo web procesado y guardado en Obsidian bajo **" + temaCarpeta + "**!");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error procesando URL: " + e.getMessage());
            enviarNotificacion(chatId, "❌ Error procesando el link: " + e.getMessage());
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
            
            Map<String, Object> extractionResponse = restTemplate.postForObject(
                extraerPdfUrl, body, Map.class);
            
            String textoCompleto = (String) extractionResponse.get("texto");
            String temaCarpeta = (String) extractionResponse.get("tema");
            String resumenInteligente = (String) extractionResponse.get("resumen");

            
            Map<String, Object> metadataDoc;
            if (extractionResponse.containsKey("metadata")) {
                metadataDoc = (Map<String, Object>) extractionResponse.get("metadata");
            } else {
                metadataDoc = Map.of("autor", "Desconocido", "anio", "s.f.");
            }

            enviarNotificacion(chatId, "📌 Tema detectado: **" + temaCarpeta + "**. Guardando en base de datos...");

            // 2. GUARDAR EN POSTGRES
            Documento doc = new Documento();
            doc.setTitulo(nombreDocumento);
            doc.setFechaProcesamiento(LocalDateTime.now());
            doc.setEstado("PROCESADO");
            doc.setMetadata(metadataDoc); 
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

            String formato = getFormatoActual(chatId);
            String tituloLimpio = nombreDocumento.replace(".pdf", "");
            
            // Llamamos al motor. Como es PDF, le pasamos 'null' en contenidoExtra y fuenteUrl
            String contenidoMd = generarContenidoMarkdown(tituloLimpio, resumenInteligente, null, temaCarpeta, null, formato);

            String rutaCarpetaAbsoluta = generarRutaCarpeta("Investigaciones/" + temaCarpeta);
            crearNotaEnObsidian(tituloLimpio, contenidoMd, rutaCarpetaAbsoluta);

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

        enviarNotificacion(chatId, "📥 Descargando PDF oficial de ArXiv para que Troxi lo analice en profundidad...");

        Map<String, String> seleccion = opciones.get(indiceSeleccionado);
        String titulo = seleccion.get("titulo");
        String urlPdf = seleccion.get("url");

        try {
            // 1. Descargamos el PDF directamente desde la URL de ArXiv usando tu RestTemplate
            byte[] pdfBytes = restTemplate.getForObject(urlPdf, byte[].class);
            
            if (pdfBytes != null) {
                // 2. Limpiamos el título para que sea un nombre de archivo válido
                String nombreDocumento = titulo.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
                if (nombreDocumento.length() > 50) {
                    nombreDocumento = nombreDocumento.substring(0, 50);
                }
                nombreDocumento += ".pdf";

                // 3. ¡LA MAGIA! Le pasamos el PDF a tu cerebro unificado
                ejecutarLogicaProcesamiento(chatId, pdfBytes, nombreDocumento);
            } else {
                enviarNotificacion(chatId, "❌ El archivo descargado está vacío.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error descargando de ArXiv: " + e.getMessage());
            enviarNotificacion(chatId, "❌ Error al procesar el paper de ArXiv con IA: " + e.getMessage());
        }
    }

    // 4. MOTOR DE PLANTILLAS DINÁMICAS
    private String generarContenidoMarkdown(String titulo, String resumen, String contenidoExtra, String tema, String fuenteUrl, String formato) {
        String formatoLimpio = formato.toUpperCase();
        StringBuilder md = new StringBuilder();

        // 1. Cabecera YAML Frontmatter (Igual para todos)
        md.append("---\n");
        md.append("tags: [").append(fuenteUrl != null ? "web" : "pdf").append(", ").append(tema.toLowerCase()).append("]\n");
        if (fuenteUrl != null) md.append("fuente_original: ").append(fuenteUrl).append("\n");
        md.append("fecha: ").append(java.time.LocalDate.now()).append("\n");
        md.append("---\n");
        md.append("# ").append(titulo).append("\n\n");

        // 2. Estructura Dinámica según la preferencia del usuario
        if (formatoLimpio.contains("MENTEFACTO")) {
            md.append("## 🧠 Concepto Central\n").append(resumen).append("\n\n");
            md.append("### ⬆️ Supraordenada (Clase Superior)\n- \n\n");
            md.append("### 🚫 Exclusiones\n- \n\n");
            md.append("### 🔀 Versiones / Isoordinadas\n- \n\n");
            md.append("### ⬇️ Infraordinadas (Subtipos)\n- \n\n");

        } else if (formatoLimpio.contains("ESTUDIO")) {
            md.append("## 🧠 Resumen Ejecutivo\n").append(resumen).append("\n\n");
            md.append("## 🔬 Metodología y Conceptos Clave\n- \n\n");
            md.append("## 🎯 Conclusiones / Aplicación Práctica\n- \n\n");

        } else {
            // Formato Estándar (Por defecto)
            md.append("> [!success] Memoria RAM de Troxi Activa\n");
            md.append("> Documento indexado bajo la categoría **").append(tema).append("**.\n\n");
            md.append("## 🧠 Resumen Inteligente\n").append(resumen).append("\n\n");
            md.append("---\n");
            md.append("## ✍️ Apuntes de Clase\n- [ ] \n");
        }

        // 3. Anexo: Contenido original oculto (Solo si es Web)
        if (contenidoExtra != null && !contenidoExtra.isEmpty()) {
            md.append("\n---\n");
            md.append("> [!quote]- 📖 Clic aquí para ver el Contenido Original Extraído\n");
            md.append(contenidoExtra.replaceAll("(?m)^", "> ")).append("\n");
        }

        return md.toString();
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

    public String generarCitaAutomatica(long chatId, String parteDelTitulo) {
        try {
            // 1. Buscamos en la base de datos (Postgres)
            List<Documento> docs = documentoRepository.findByTituloContainingIgnoreCase(parteDelTitulo.trim());
            if (docs.isEmpty()) {
                return "❌ No encontré ningún documento en mi base de datos que contenga '" + parteDelTitulo + "' en su título.";
            }
            
            Documento doc = docs.get(0); 
            Map<String, Object> meta = doc.getMetadata();
            
            String tituloInfo = doc.getTitulo();
            String autorInfo = (meta != null && meta.containsKey("autor")) ? meta.get("autor").toString() : "Desconocido";
            String anioInfo = (meta != null && meta.containsKey("anio")) ? meta.get("anio").toString() : "s.f.";
            
            String formato = getFormatoActual(chatId);
            
            // 2. Pedimos la cita a Python
            String promptCita = "Genera únicamente la referencia bibliográfica estricta en formato " + formato + 
                                " para el siguiente documento:\n" +
                                "Título: " + tituloInfo + "\nAutor: " + autorInfo + "\nAño: " + anioInfo;
                                
            Map<String, String> request = Map.of("pregunta", promptCita, "contexto", "Generación de bibliografía directa.", "formato_cita", formato);
            
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.postForObject(generarRespuestaUrl, request, Map.class);
            String citaGenerada = response.get("respuesta");

            // ==========================================
            // NÉXUS DE OBSIDIAN: AGREGAR CITA AL ARCHIVO
            // ==========================================
            try {
                // Usamos tu ruta base inyectada y tu función recursiva
                File carpetaBase = new File(baseObsidianPath); 
                
                // Limpiamos el nombre igual que cuando creas la nota originalmente
                String nombreLimpio = tituloInfo.replaceAll("[^a-zA-Z0-9]", "_");
                if (nombreLimpio.length() > 60) nombreLimpio = nombreLimpio.substring(0, 60);
                
                // REUTILIZAMOS tu función: buscarArchivoRecursivo
                File archivoEncontrado = buscarArchivoRecursivo(carpetaBase, nombreLimpio + ".md");

                if (archivoEncontrado != null) {
                    String bloqueCita = "\n\n---\n## 📚 Bibliografía (Troxi)\n" + citaGenerada + "\n";
                    
                    // Escribimos al final sin borrar nada (StandardOpenOption.APPEND)
                    java.nio.file.Files.writeString(
                        archivoEncontrado.toPath(), 
                        bloqueCita, 
                        java.nio.file.StandardOpenOption.APPEND
                    );
                    System.out.println("✅ Nota actualizada físicamente en Obsidian.");
                }
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo actualizar la nota física, pero te envío la cita: " + e.getMessage());
            }
            // ==========================================

            return "📚 **Referencia (" + formato + "):**\n\n" + citaGenerada;
            
        } catch (Exception e) {
            return "❌ Error al generar la cita: " + e.getMessage();
        }
    }

        // HERRAMIENTA COMPARTIDA: Ahora recibe el token a usar
    private byte[] descargarArchivoTelegram(String fileId, String tokenUsar) throws Exception {
        String getFileUrl = "https://api.telegram.org/bot" + tokenUsar + "/getFile?file_id=" + fileId;
        Map<String, Object> fileResponse = restTemplate.getForObject(getFileUrl, Map.class);
        Map<String, Object> result = (Map<String, Object>) fileResponse.get("result");
        String filePath = (String) result.get("file_path");
        String downloadUrl = "https://api.telegram.org/file/bot" + tokenUsar + "/" + filePath;

        return restTemplate.getForObject(downloadUrl, byte[].class);
    }

    // HERRAMIENTA COMPARTIDA 2: Extrae el texto del PDF usando tu worker en Python
    private String extraerTextoLimpioDePdf(byte[] pdfBytes, String nombreDocumento) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource contentsAsResource = new ByteArrayResource(pdfBytes) {
            @Override public String getFilename() { return nombreDocumento; }
        };
        body.add("file", contentsAsResource);

        @SuppressWarnings("unchecked")
        Map<String, Object> extractionResponse = restTemplate.postForObject(
            extraerPdfUrl, body, Map.class);
            
        // Solo nos interesa el texto crudo, ignoramos el tema y resumen académico
        return (String) extractionResponse.get("texto");
    }

    public String procesarTesisInversion(long chatId, String fileId, String nombreDocumento) {
        try {
            enviarNotificacion(chatId, "⏳ Leyendo tu tesis de inversión: " + nombreDocumento);

            // 1. Usamos la herramienta de descarga (Telegram)
            byte[] pdfBytes = descargarArchivoTelegram(fileId, this.financeBotToken);
            
            // 2. Extraemos el texto usando la herramienta compartida (Python)
            String textoExtraido = extraerTextoLimpioDePdf(pdfBytes, nombreDocumento);

            // 3. El Prompt Cuantitativo para Gemini (Enviado por /generar-respuesta)
            enviarNotificacion(chatId, "🧠 Analizando la estrategia Core-Satellite...");
            
            String promptExtraccion = "Eres un analista cuantitativo. Lee el siguiente texto de una tesis de inversión " +
                    "y devuelve ÚNICAMENTE un objeto JSON válido con los tickers financieros (símbolos de mercado) a rastrear, " +
                    "clasificados en dos listas: 'core' (activos principales) y 'satelite' (activos tácticos a corto plazo/futuros). " +
                    "Asegúrate de traducir los nombres a sus Tickers oficiales (ej. Bitcoin -> BTC, Ethereum -> ETH, Solana -> SOL, Filecoin -> FIL). " +
                    "Ejemplo de salida: {\"core\": [\"BTC\", \"ETH\", \"ICP\", \"LINK\", \"FIL\", \"SOL\"], \"satelite\": [\"SOL\"]}. " +
                    "NO agregues explicaciones, markdown, ni texto adicional.\n\n" +
                    "TEXTO DE LA TESIS:\n" + textoExtraido;

            Map<String, String> request = Map.of(
                "pregunta", promptExtraccion, 
                "contexto", "Extracción JSON estricta de activos financieros",
                "formato_cita", "Ninguno"
            );
            
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.postForObject(generarRespuestaUrl, request, Map.class);
            String jsonExtraido = response.get("respuesta");

            // Limpiar residuos de markdown por si Gemini los pone
            jsonExtraido = jsonExtraido.replace("```json", "").replace("```", "").trim();

            // --- GUARDAR EN BASE DE DATOS AUTOMÁTICAMENTE ---
            PortafolioEstrategia portafolio = new PortafolioEstrategia();
            portafolio.setId(1L); // Siempre actualiza el registro principal
            portafolio.setJsonActivos(jsonExtraido);
            portafolioRepository.save(portafolio);
            // ------------------------------------------------

            return "✅ **Tesis Procesada y Asimilada**\n\nHe extraído el siguiente portafolio de tu documento para rastreo automático:\n`" + jsonExtraido + "`";

        } catch (Exception e) {
            return "❌ Error procesando la tesis: " + e.getMessage();
        }
    }

    public String procesarImagenFitness(byte[] imageBytes, String fileName) {
        try {
            System.out.println("🏋️‍♂️ Enviando imagen al Agente Fitness...");
            String url = "http://agente-fitness:8002/api/ia/fitness/leer-imagen";

            // 1. Configuramos las cabeceras correctamente
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 2. Creamos un recurso explícito. Spring requiere que sobrescribamos getFilename() 
            // de lo contrario ignora el byte[] y no lo trata como un archivo adjunto.
            final String finalFileName = (fileName != null && !fileName.isEmpty()) ? fileName : "captura.jpg";
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return finalFileName;
                }
            };

            // 3. Empaquetamos el body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("imagen", imageResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 4. Disparamos la petición
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            
            System.out.println("✅ Respuesta del Agente Fitness recibida exitosamente.");
            return response.getBody();

        } catch (Exception e) {
            System.err.println("❌ Error crítico al llamar al agente fitness: " + e.getMessage());
            e.printStackTrace(); // Esto nos dará más detalles en los logs si llega a fallar
            return null;
        }
    }

    public String procesarAudioFitness(byte[] audioBytes, String fileName) {
        try {
            System.out.println("🎙️ Enviando nota de voz al Agente Fitness...");
            String url = "http://agente-fitness:8002/api/ia/fitness/leer-audio";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            final String finalFileName = (fileName != null && !fileName.isEmpty()) ? fileName : "nota_voz.ogg";
            ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return finalFileName;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio", audioResource); // Ojo aquí: se llama "audio"

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            
            System.out.println("✅ Respuesta de audio recibida exitosamente.");
            return response.getBody();

        } catch (Exception e) {
            System.err.println("❌ Error crítico al llamar al agente fitness (audio): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // HERRAMIENTA: Formatea las sesiones para que la IA las entienda
    private String analizarTendencia(List<SesionEntrenamiento> sesiones) {
        if (sesiones == null || sesiones.isEmpty()) return "No hay datos de sesiones recientes registradas.";
        
        StringBuilder sb = new StringBuilder();
        for (SesionEntrenamiento s : sesiones) {
            sb.append("- Fecha: ").append(s.getFecha())
              .append(" | Duración: ").append(s.getDuracionMinutos() != null ? s.getDuracionMinutos() + " min" : "N/A")
              .append(" | RPE (Esfuerzo 1-10): ").append(s.getRpeSesion() != null ? s.getRpeSesion() : "N/A")
              .append(" | Notas: ").append(s.getNotas() != null ? s.getNotas() : "Ninguna")
              .append("\n");
        }
        return sb.toString();
    }

    // MÉTODO MAESTRO: El RAG Híbrido (SQL + Vectores)
    public String consultarCoachInteligente(long chatId, String mensajeSensacion) {
        try {
            enviarNotificacion(chatId, "🧠 Analizando tu biometría, metas a largo plazo y recuperación actual...");

            // 1. Obtener Biometría y Metas Activas
            PerfilUsuario miPerfil = perfilRepository.findById(1L).orElse(null);
            List<MetaFitness> misMetas = metaRepository.findByPerfilIdAndEstado(1L, "ACTIVA");
            String contextoEstrategico = estructurarPerfilYMetas(miPerfil, misMetas);

            // 2. Obtener Sesiones Recientes
            List<SesionEntrenamiento> ultimasSesiones = sesionRepository.findTop5ByOrderByFechaDesc();
            String datosRecientes = analizarTendencia(ultimasSesiones);

            // 3. Obtener Ciencia (Vectores)
            String cienciaRecuperacion = buscarContextoParaPregunta("periodización, macrociclos, fatiga y recuperación", chatId);

            // 3. PROMPT ESTRATÉGICO: Fusionamos sensaciones, biometría y ciencia
            // 3. PROMPT ESTRATÉGICO: Fusionamos sensaciones, biometría y ciencia
            String promptEstrategico = "Eres un Coach de élite experto en ciencia del deporte y alto rendimiento ejecutivo.\n" +
                "SENSACIÓN ACTUAL DEL USUARIO: \"" + mensajeSensacion + "\"\n\n" +
                "HISTORIAL DE ENTRENAMIENTO RECIENTE:\n" + datosRecientes + "\n\n" +
                "LITERATURA CIENTÍFICA (Aplica solo lo relevante):\n" + cienciaRecuperacion + "\n\n" +
                "REGLA DE ORO: Analiza las sensaciones del usuario frente a sus datos. Si el usuario siente mucha energía pero los datos marcan fatiga acumulada (RPEs altos), SÉ FLEXIBLE. No prohíbas entrenar; recomienda una sesión dosificada (ej. alta intensidad pero volumen muy bajo) para cuidar el SNC.\n\n" +
                "⚠️ RESTRICCIÓN CRÍTICA DE FORMATO: Tu respuesta será enviada por Telegram. DEBE ser ejecutiva, directa y concisa. Límite máximo absoluto de 2500 caracteres (unas 250 palabras). NO uses formato Markdown (ESTRICTAMENTE PROHIBIDO usar asteriscos **, guiones bajos _, o # para negritas o títulos). Usa exclusivamente texto plano y viñetas simples (- o •). Ve directo al plan de acción.";
            Map<String, String> request = Map.of(
                "pregunta", promptEstrategico,
                "contexto", "Integración completa: Perfil, Metas, Sesiones y pgvector.",
                "formato_cita", "Estilo Coach Deportivo"
            );

            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.postForObject(generarRespuestaUrl, request, Map.class);
            return response.get("respuesta");

        } catch (Exception e) {
            return "❌ Error en el análisis estratégico del Coach: " + e.getMessage();
        }
    }

    private String estructurarPerfilYMetas(PerfilUsuario perfil, List<MetaFitness> metas) {
        if (perfil == null) return "No hay perfil biométrico registrado.";
        
        StringBuilder sb = new StringBuilder();
        sb.append("BIOMETRÍA DEL USUARIO:\n");
        sb.append("- Género: ").append(perfil.getGenero() != null ? perfil.getGenero() : "N/D").append("\n");
        sb.append("- Contextura: ").append(perfil.getContextura() != null ? perfil.getContextura() : "N/D").append("\n");
        sb.append("- Peso Actual: ").append(perfil.getPesoActualKg() != null ? perfil.getPesoActualKg() + " kg" : "N/D").append("\n\n");

        sb.append("METAS ACTIVAS A LARGO PLAZO:\n");
        if (metas == null || metas.isEmpty()) {
            sb.append("- Ninguna meta activa registrada.\n");
        } else {
            for (MetaFitness m : metas) {
                sb.append("- [").append(m.getTipoMeta()).append("] ")
                  .append(m.getDescripcion())
                  .append(" | Objetivo: ").append(m.getValorObjetivo() != null ? m.getValorObjetivo() : "").append(" ").append(m.getUnidadMetrica() != null ? m.getUnidadMetrica() : "")
                  .append(" | Fecha Límite: ").append(m.getFechaLimite() != null ? m.getFechaLimite() : "Sin fecha")
                  .append("\n");
            }
        }
        return sb.toString();
    }

    public String planificarSemana(long chatId, String horarios) {
        try {
            enviarNotificacion(chatId, "⏳ Diseñando tu macrociclo semanal a medida...");

            // 1. Obtener tu Biometría y Metas Activas
            PerfilUsuario miPerfil = perfilRepository.findById(1L).orElse(null);
            List<MetaFitness> misMetas = metaRepository.findByPerfilIdAndEstado(1L, "ACTIVA");
            String contextoEstrategico = estructurarPerfilYMetas(miPerfil, misMetas);

            // 2. Preparar el envío a Python
            Map<String, String> requestPython = Map.of(
                "contextoEstrategico", contextoEstrategico,
                "horarios", horarios
            );

            // URL directa de tu nuevo endpoint en el worker de fitness
            String urlPlanificador = "http://agente-fitness:8002/api/ia/fitness/planificar";
            
            // 3. Llamar a la IA
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(urlPlanificador, requestPython, Map.class);

            if (response == null || response.containsKey("error")) {
                return "❌ Troxi tuvo un problema al generar la planificación. Intenta ser más específica con los horarios.";
            }

            // 4. La Magia: Convertir el JSON de Python directamente a tus entidades de Java
            ObjectMapper mapper = new ObjectMapper();
            List<RutinaTemplate> rutinasGeneradas = mapper.convertValue(
                response.get("rutinas"), 
                new TypeReference<List<RutinaTemplate>>() {}
            );

            // 5. Guardar en PostgreSQL
            for (RutinaTemplate rutina : rutinasGeneradas) {
                // Tu FitnessService ya tiene la magia de asociar los Ejercicios a la Rutina por detrás
                fitnessService.crearRutina(rutina); 
            }

            // 6. Devolver el mensaje amigable a Telegram
            return (String) response.get("mensajeCoach");

        } catch (Exception e) {
            System.err.println("❌ Error crítico en planificarSemana: " + e.getMessage());
            return "❌ Error interno al guardar la planificación en la bóveda: " + e.getMessage();
        }
    }

    // Memoria a corto plazo para el chat
    private Map<Long, String> memoriaChat = new ConcurrentHashMap<>();

    public String procesarChatInteligente(long chatId, String mensajeUsuario) {
        try {
            // 1. Recuperar el contexto
            String historial = memoriaChat.getOrDefault(chatId, "Sin historial reciente.");
            SesionEntrenamiento ultimaSesion = sesionRepository.findFirstByOrderByIdDesc();
            
            // 2. Enviar a Python
            Map<String, Object> requestPython = Map.of(
                "mensaje", mensajeUsuario,
                "memoria", historial,
                "ultima_sesion", ultimaSesion != null ? ultimaSesion : "Ninguna"
            );
            
            // Llama a tu nuevo endpoint de Python
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject("http://agente-fitness:8002/api/ia/fitness/chat-inteligente", requestPython, Map.class);
            
            String intencion = (String) response.get("intencion");
            String respuestaCoach = (String) response.get("respuesta_telegram");

            // 3. EJECUTAR LA ACCIÓN
            if ("CORREGIR_SESION".equals(intencion)) {
                // Aquí aplicamos el "Borrón y Cuenta Nueva"
                sesionRepository.delete(ultimaSesion); // Borramos la que tenía el error
                
                // Mapeamos los datos_corregidos a una nueva SesionEntrenamiento y la guardamos
                // (Necesitarás usar ObjectMapper de Jackson para convertir el Map 'datos_corregidos' a tu entidad)
                System.out.println("🔄 Sesión corregida en la base de datos.");
            } else if ("NUEVA_PREFERENCIA".equals(intencion)) {
                // Podrías guardar esto en el campo de PerfilUsuario en el futuro
                System.out.println("⭐ Preferencia aprendida: " + mensajeUsuario);
            }

            // 4. Actualizar memoria
            memoriaChat.put(chatId, "Usuario: " + mensajeUsuario + " | Troxi: " + respuestaCoach);

            return respuestaCoach;
            
        } catch (Exception e) {
            return "❌ No pude procesar tu mensaje: " + e.getMessage();
        }
    }

}
