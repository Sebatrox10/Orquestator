package com.sebatrox.orquestador.controller;

import com.sebatrox.orquestador.service.OrquestadorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

//@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final OrquestadorService orquestadorService;
    private final String botToken;
    private final String botUsername;

    public TelegramBot(OrquestadorService orquestadorService, 
                       @Value("${telegram.bot.token}") String botToken,
                       @Value("${telegram.bot.username}") String botUsername) {
        super(botToken);
        this.orquestadorService = orquestadorService;
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        
        // ==========================================
        // CASO 1: CLICS EN LOS BOTONES DE ARXIV
        // ==========================================
        if (update.hasCallbackQuery()) {
            String callData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callData.startsWith("guardar_arxiv_")) {
                int indice = Integer.parseInt(callData.replace("guardar_arxiv_", ""));
                enviarMensaje(chatId, "✅ ¡Entendido! Creando la estructura en tu Obsidian...");

                new Thread(() -> {
                    orquestadorService.procesarSeleccionArxiv(chatId, indice);
                    enviarMensaje(chatId, "📂 ¡Nota académica creada con éxito! Ya debería estar sincronizada.");
                }).start();
            }
            return;
        }

        if (!update.hasMessage()) return;
        long chatId = update.getMessage().getChatId();

        // ==========================================
        // CASO 2: RECIBIR UN PDF (Apuntes de clase)
        // ==========================================
        if (update.getMessage().hasDocument()) {
            String mimeType = update.getMessage().getDocument().getMimeType();
            if ("application/pdf".equals(mimeType)) {
                String fileId = update.getMessage().getDocument().getFileId();
                String fileName = update.getMessage().getDocument().getFileName();

                enviarMensaje(chatId, "📄 ¡Recibido! Procesando '" + fileName + "'...");
                
                new Thread(() -> {
                    boolean exito = orquestadorService.procesarDocumentoPdf(chatId, fileId, fileName);
                    if (exito) enviarMensaje(chatId, "✅ Documento indexado en tu base de datos y nota creada en Obsidian.");
                    else enviarMensaje(chatId, "❌ Hubo un problema al procesar el documento.");
                }).start();
            }
            return;
        }

        // ==========================================
        // CASO 3: MENSAJES DE TEXTO
        // ==========================================
        if (update.getMessage().hasText()) {
            String textoUsuario = update.getMessage().getText();


            if (textoUsuario.toLowerCase().startsWith("/editar ")) {
                // Extraemos "/editar nombre_archivo instruccion"
                String resto = textoUsuario.substring(8).trim();
                int espacio = resto.indexOf(" ");
                
                if (espacio == -1) {
                    enviarMensaje(chatId, "⚠️ Formato: /editar [nombre_archivo] [qué quieres hacer]");
                    return;
                }
                
                String nombreNota = resto.substring(0, espacio).trim();
                String queHacer = resto.substring(espacio).trim();
                
                enviarMensaje(chatId, "🤖 Buscando '" + nombreNota + "' para editarla según tus instrucciones...");
                
                new Thread(() -> {
                    String resultado = orquestadorService.editarNotaObsidian(nombreNota, queHacer);
                    enviarMensaje(chatId, resultado);
                }).start();
                return; 
            }

            // SUB-CASO A: COMANDO EXPLÍCITO PARA BUSCAR EN INTERNET
            if (textoUsuario.toLowerCase().startsWith("/arxiv ")) {
                String temaParaBuscar = textoUsuario.substring(7).trim(); 
                enviarMensaje(chatId, "🔍 Investigando en ArXiv sobre: " + temaParaBuscar + "...");
                
                new Thread(() -> {
                    // 1. Obtenemos la lista desde el orquestador
                    List<Map<String, String>> opciones = orquestadorService.investigarEnArxiv(chatId, temaParaBuscar);
                    
                    if (opciones == null || opciones.isEmpty()) {
                        enviarMensaje(chatId, "❌ No encontré resultados en ArXiv para: " + temaParaBuscar);
                        return;
                    }

                    // 2. Armamos el texto con los títulos numerados
                    StringBuilder textoRespuesta = new StringBuilder("Sebas, encontré estas joyas científicas. ¿Cuál quieres que procese para tu Obsidian?\n\n");
                    
                    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

                    for (int i = 0; i < opciones.size(); i++) {
                        Map<String, String> paper = opciones.get(i);
                        textoRespuesta.append(i + 1).append(". ").append(paper.get("titulo")).append("\n\n");

                        // 3. Creamos el botón para cada opción
                        InlineKeyboardButton boton = new InlineKeyboardButton();
                        boton.setText("📥 Guardar Opción " + (i + 1));
                        boton.setCallbackData("guardar_arxiv_" + i);
                        
                        List<InlineKeyboardButton> rowInline = new ArrayList<>();
                        rowInline.add(boton);
                        rowsInline.add(rowInline);
                    }
                    markupInline.setKeyboard(rowsInline);

                    // 4. Enviamos el paquete completo (Texto + Botones) a Telegram
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText(textoRespuesta.toString());
                    message.setReplyMarkup(markupInline);

                    try {
                        execute(message); 
                    } catch (Exception e) {
                        System.err.println("❌ Error enviando botones a Telegram: " + e.getMessage());
                    }
                }).start();
            } 
            
            // SUB-CASO B: CHAT NORMAL Y PREGUNTAS SOBRE TUS PDFS
            else {
                // Aquí llamas a tu función de RAG que busca en base de datos y habla con Gemini
                new Thread(() -> {
                    // Reemplaza esto por tu función real de responder preguntas
                    String respuesta = orquestadorService.buscarContextoParaPregunta(textoUsuario, chatId); 
                    enviarMensaje(chatId, respuesta);
                }).start();
            }
        }
    }


    private void enviarMensaje(long chatId, String texto) {
        SendMessage message = new SendMessage(String.valueOf(chatId), texto);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Lógica para enviar las 5 opciones con botones
    private void enviarOpcionesInvestigacion(long chatId, List<Map<String, String>> opciones) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < opciones.size(); i++) {
            Map<String, String> opc = opciones.get(i);
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText((i + 1) + ". " + opc.get("titulo"));
            btn.setCallbackData("guardar_arxiv_" + i); // Java recordará qué opción es cuál
            
            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);
        SendMessage message = new SendMessage(String.valueOf(chatId), "Sebas, encontré estas 5 joyas científicas. ¿Cuál quieres que procese para tu Obsidian?");
        message.setReplyMarkup(markup);
        try {
            execute(message); 
        } catch (TelegramApiException e) {
            System.err.println("Error enviando botones: " + e.getMessage());
        }
    }
}
