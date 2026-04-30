package com.sebatrox.orquestador.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "sesiones_entrenamiento")
public class SesionEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private Integer duracionMinutos;
    
    // Biofeedback y Esfuerzo
    private Integer rpeSesion; // Rating of Perceived Exertion (1 al 10)
    private String notas; // Ej: "Semana pesada en el trabajo, bajé los pesos un 10%"

    // Relación: ¿En qué plantilla te basaste hoy? (Puede ser null si fue un entrenamiento libre)
    @ManyToOne
    @JoinColumn(name = "rutina_template_id")
    private RutinaTemplate rutinaBase;

    // Relación: Los ejercicios que realmente hiciste hoy
    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL)
    private List<RegistroEjercicio> ejerciciosRealizados;

    // Getters y Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public Integer getRpeSesion() { return rpeSesion; }
    public void setRpeSesion(Integer rpeSesion) { this.rpeSesion = rpeSesion; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public RutinaTemplate getRutinaBase() { return rutinaBase; }
    public void setRutinaBase(RutinaTemplate rutinaBase) { this.rutinaBase = rutinaBase; }
    public List<RegistroEjercicio> getEjerciciosRealizados() { return ejerciciosRealizados; }
    public void setEjerciciosRealizados(List<RegistroEjercicio> ejerciciosRealizados) { this.ejerciciosRealizados = ejerciciosRealizados; }

    @PrePersist
    protected void onCreate() {
        if (this.fecha == null) {
            this.fecha = LocalDate.now();
        }
    }
}
