package com.sebatrox.orquestador.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ejercicio_templates")
public class EjercicioTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreEjercicio; // Ej: "Sentadilla Búlgara"
    private Integer series;
    private Integer repeticionesBase;
    private Double pesoSugerido;
    private Integer descansoSegundos;
    private Integer orden; // Para saber qué va primero

    @ManyToOne
    @JoinColumn(name = "rutina_id")
    @JsonIgnore
    private RutinaTemplate rutinaTemplate;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getNombreEjercicio() {
        return nombreEjercicio;
    }
    public void setNombreEjercicio(String nombreEjercicio) {
        this.nombreEjercicio = nombreEjercicio;
    }
    public Integer getSeries() {
        return series;
    }
    public void setSeries(Integer series) {
        this.series = series;
    }
    public Integer getRepeticionesBase() {
        return repeticionesBase;
    }
    public void setRepeticionesBase(Integer repeticionesBase) {
        this.repeticionesBase = repeticionesBase;
    }
    public Double getPesoSugerido() {
        return pesoSugerido;
    }
    public void setPesoSugerido(Double pesoSugerido) {
        this.pesoSugerido = pesoSugerido;
    }
    public Integer getDescansoSegundos() {
        return descansoSegundos;
    }
    public void setDescansoSegundos(Integer descansoSegundos) {
        this.descansoSegundos = descansoSegundos;
    }
    public Integer getOrden() {
        return orden;
    }
    public void setOrden(Integer orden) {
        this.orden = orden;
    }
    public RutinaTemplate getRutinaTemplate() {
        return rutinaTemplate;
    }
    public void setRutinaTemplate(RutinaTemplate rutinaTemplate) {
        this.rutinaTemplate = rutinaTemplate;
    }
    
}
