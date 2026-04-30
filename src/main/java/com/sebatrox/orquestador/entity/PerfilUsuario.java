package com.sebatrox.orquestador.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfil_usuario")
public class PerfilUsuario {
    @Id
    private Long id = 1L; // Solo tú

    private Double pesoActual;
    private Double porcentajeGrasa;
    private Double masaMuscularKg;
    
    // Metas (Ganchos para la IA)
    private Double metaPeso;
    private LocalDate fechaMeta;

    // --- Aquí se conectará la Nutrición después ---
    // private Double caloriasObjetivo;
    // private Double proteinaGrapas;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Double getPesoActual() {
        return pesoActual;
    }
    public void setPesoActual(Double pesoActual) {
        this.pesoActual = pesoActual;
    }
    public Double getPorcentajeGrasa() {
        return porcentajeGrasa;
    }
    public void setPorcentajeGrasa(Double porcentajeGrasa) {
        this.porcentajeGrasa = porcentajeGrasa;
    }
    public Double getMasaMuscularKg() {
        return masaMuscularKg;
    }
    public void setMasaMuscularKg(Double masaMuscularKg) {
        this.masaMuscularKg = masaMuscularKg;
    }
    public Double getMetaPeso() {
        return metaPeso;
    }
    public void setMetaPeso(Double metaPeso) {
        this.metaPeso = metaPeso;
    }
    public LocalDate getFechaMeta() {
        return fechaMeta;
    }
    public void setFechaMeta(LocalDate fechaMeta) {
        this.fechaMeta = fechaMeta;
    }
    
}
