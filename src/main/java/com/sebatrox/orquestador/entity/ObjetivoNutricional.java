package com.sebatrox.orquestador.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "objetivos_nutricionales")
public class ObjetivoNutricional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación directa con tu perfil existente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilUsuario perfilUsuario;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "calorias_diarias")
    private Integer caloriasDiarias;

    @Column(name = "proteinas_g")
    private Integer proteinasGramos;

    @Column(name = "carbohidratos_g")
    private Integer carbohidratosGramos;

    @Column(name = "grasas_g")
    private Integer grasasGramos;

    private String estado; // EJ: "ACTIVO", "HISTORICO"

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PerfilUsuario getPerfilUsuario() { return perfilUsuario; }
    public void setPerfilUsuario(PerfilUsuario perfilUsuario) { this.perfilUsuario = perfilUsuario; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public Integer getCaloriasDiarias() { return caloriasDiarias; }
    public void setCaloriasDiarias(Integer caloriasDiarias) { this.caloriasDiarias = caloriasDiarias; }
    public Integer getProteinasGramos() { return proteinasGramos; }
    public void setProteinasGramos(Integer proteinasGramos) { this.proteinasGramos = proteinasGramos; }
    public Integer getCarbohidratosGramos() { return carbohidratosGramos; }
    public void setCarbohidratosGramos(Integer carbohidratosGramos) { this.carbohidratosGramos = carbohidratosGramos; }
    public Integer getGrasasGramos() { return grasasGramos; }
    public void setGrasasGramos(Integer grasasGramos) { this.grasasGramos = grasasGramos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}