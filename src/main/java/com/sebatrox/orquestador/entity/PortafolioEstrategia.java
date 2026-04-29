package com.sebatrox.orquestador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "portafolio_estrategia")
public class PortafolioEstrategia {

    // Usaremos un ID fijo (ej. 1) porque solo queremos mantener el portafolio ACTUAL activo.
    // Cuando subas un nuevo PDF, simplemente sobreescribirá esta misma fila.
    @Id
    private Long id = 1L;

    @Column(columnDefinition = "TEXT")
    private String jsonActivos;

    // --- Getters y Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJsonActivos() {
        return jsonActivos;
    }

    public void setJsonActivos(String jsonActivos) {
        this.jsonActivos = jsonActivos;
    }
}