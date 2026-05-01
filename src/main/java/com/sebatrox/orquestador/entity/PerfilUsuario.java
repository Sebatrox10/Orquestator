package com.sebatrox.orquestador.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "perfil_usuario")
public class PerfilUsuario {
    @Id
    private Long id = 1L; // Solo tú

    // --- Demografía y Genética ---
    private LocalDate fechaNacimiento; // Para que la IA calcule tu edad exacta siempre
    private String genero; 
    private Double alturaCm;
    private String contextura; // Ej: "Mesomorfo", "Ectomorfo"

    // --- Biometría Dinámica ---
    private Double pesoActualKg;
    private Double porcentajeGrasa;
    private Double masaMuscularKg;

    // Relación: Un perfil puede tener muchas metas
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MetaFitness> metas;

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public Double getAlturaCm() { return alturaCm; }
    public void setAlturaCm(Double alturaCm) { this.alturaCm = alturaCm; }
    public String getContextura() { return contextura; }
    public void setContextura(String contextura) { this.contextura = contextura; }
    public Double getPesoActualKg() { return pesoActualKg; }
    public void setPesoActualKg(Double pesoActualKg) { this.pesoActualKg = pesoActualKg; }
    public Double getPorcentajeGrasa() { return porcentajeGrasa; }
    public void setPorcentajeGrasa(Double porcentajeGrasa) { this.porcentajeGrasa = porcentajeGrasa; }
    public Double getMasaMuscularKg() { return masaMuscularKg; }
    public void setMasaMuscularKg(Double masaMuscularKg) { this.masaMuscularKg = masaMuscularKg; }
    public List<MetaFitness> getMetas() { return metas; }
    public void setMetas(List<MetaFitness> metas) { this.metas = metas; }
}