package com.sebatrox.orquestador.controller;

import com.sebatrox.orquestador.service.OrquestadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private OrquestadorService orquestadorService;

    @PostMapping("/procesar-tesis")
    public ResponseEntity<Map<String, String>> procesarTesisFinanciera(@RequestBody Map<String, Object> body) {
        long chatId = Long.parseLong(body.get("chatId").toString());
        String fileId = body.get("fileId").toString(); 
        String fileName = body.getOrDefault("fileName", "tesis.pdf").toString(); 
        
        String respuesta = orquestadorService.procesarTesisInversion(chatId, fileId, fileName);
        return ResponseEntity.ok(Map.of("data", respuesta));
    }
}