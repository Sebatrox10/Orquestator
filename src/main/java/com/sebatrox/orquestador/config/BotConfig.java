package com.sebatrox.orquestador.config;

import com.sebatrox.orquestador.controller.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

//@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException {
        // Forzamos la creación de la sesión de Telegram
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            System.out.println("⚙️ [CONFIG] Registrando el bot en los servidores de Telegram...");
            botsApi.registerBot(telegramBot);
            System.out.println("✅ [CONFIG] Registro exitoso. ¡Ahora sí debería responder!");
        } catch (TelegramApiException e) {
            System.err.println("❌ [CONFIG] Error al registrar el bot: " + e.getMessage());
        }
        return botsApi;
    }
}