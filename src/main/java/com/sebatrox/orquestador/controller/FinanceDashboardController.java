package com.sebatrox.orquestador.controller;

import com.sebatrox.orquestador.entity.PortafolioEstrategia;
import com.sebatrox.orquestador.repository.PortafolioEstrategiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*") // Permite que React se conecte sin bloqueos de CORS
public class FinanceDashboardController {

    @Autowired
    private PortafolioEstrategiaRepository portafolioRepository;

    @Autowired
    private RestTemplate restTemplate;

    // 1. Obtener la composición del portafolio (lo que extrajo Gemini)
    @GetMapping("/portafolio")
    public PortafolioEstrategia getPortafolio() {
        return portafolioRepository.findById(1L).orElse(null);
    }

    // 2. Obtener precios en tiempo real (Proxy hacia tu Python)
    @GetMapping("/precios-actuales")
    public Map<String, Object> getPreciosActuales() {
        String urlPython = "http://bot-financiero:8001/precios-cripto"; 
        return restTemplate.getForObject(urlPython, Map.class);
    }
}
