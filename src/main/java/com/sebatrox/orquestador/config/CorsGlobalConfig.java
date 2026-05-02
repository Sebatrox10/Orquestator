/* 
package com.sebatrox.orquestador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsGlobalConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // Permite que cualquier IP/localhost se conecte
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Autoriza el DELETE
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
*/