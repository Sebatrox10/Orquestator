package com.sebatrox.orquestador.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "registros_ejercicio")
public class RegistroEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreEjercicio;
    
    // Métricas de Fuerza (Pesas / Calistenia)
    private Integer seriesRealizadas;
    private String repeticionesStr; // Ej: "12, 10, 10, 8"
    private String pesosStr; // Ej: "80, 85, 90, 90"
    
    // Métricas de Cardio (Para cuando conectes el Polar o Xiaomi)
    private Double distanciaKm;
    private Integer tiempoSegundos;
    private Integer frecuenciaCardiacaMedia;

    // Relación de vuelta a la Sesión (¡Con JsonIgnore para evitar el bucle de las 2000 líneas!)
    @ManyToOne
    @JoinColumn(name = "sesion_id")
    @JsonIgnore
    private SesionEntrenamiento sesion;

    // Getters y Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreEjercicio() { return nombreEjercicio; }
    public void setNombreEjercicio(String nombreEjercicio) { this.nombreEjercicio = nombreEjercicio; }
    public Integer getSeriesRealizadas() { return seriesRealizadas; }
    public void setSeriesRealizadas(Integer seriesRealizadas) { this.seriesRealizadas = seriesRealizadas; }
    public String getRepeticionesStr() { return repeticionesStr; }
    public void setRepeticionesStr(String repeticionesStr) { this.repeticionesStr = repeticionesStr; }
    public String getPesosStr() { return pesosStr; }
    public void setPesosStr(String pesosStr) { this.pesosStr = pesosStr; }
    public Double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Double distanciaKm) { this.distanciaKm = distanciaKm; }
    public Integer getTiempoSegundos() { return tiempoSegundos; }
    public void setTiempoSegundos(Integer tiempoSegundos) { this.tiempoSegundos = tiempoSegundos; }
    public Integer getFrecuenciaCardiacaMedia() { return frecuenciaCardiacaMedia; }
    public void setFrecuenciaCardiacaMedia(Integer frecuenciaCardiacaMedia) { this.frecuenciaCardiacaMedia = frecuenciaCardiacaMedia; }
    public SesionEntrenamiento getSesion() { return sesion; }
    public void setSesion(SesionEntrenamiento sesion) { this.sesion = sesion; }
}
