package com.sebatrox.orquestador.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "metas_fitness")
public class MetaFitness {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoMeta; // Ej: "FUERZA", "CARDIO", "COMPOSICION"
    private String descripcion; // Ej: "Media Maratón Quito", "100kg Sentadilla"
    
    private Double valorObjetivo; // Ej: 21.0
    private String unidadMetrica; // Ej: "km", "kg", "%"
    
    private LocalDate fechaLimite;
    private String estado; // Ej: "ACTIVA", "LOGRADA"

    @ManyToOne
    @JoinColumn(name = "perfil_id")
    @JsonIgnore // Evita bucles infinitos al devolver JSON
    private PerfilUsuario perfil;

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipoMeta() { return tipoMeta; }
    public void setTipoMeta(String tipoMeta) { this.tipoMeta = tipoMeta; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getValorObjetivo() { return valorObjetivo; }
    public void setValorObjetivo(Double valorObjetivo) { this.valorObjetivo = valorObjetivo; }
    public String getUnidadMetrica() { return unidadMetrica; }
    public void setUnidadMetrica(String unidadMetrica) { this.unidadMetrica = unidadMetrica; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public PerfilUsuario getPerfil() { return perfil; }
    public void setPerfil(PerfilUsuario perfil) { this.perfil = perfil; }
}