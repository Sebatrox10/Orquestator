package com.sebatrox.orquestador.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consumo_suplementos")
public class ConsumoSuplemento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilUsuario perfilUsuario;

    private LocalDateTime fechaHora;

    @Column(name = "nombre_suplemento")
    private String nombreSuplemento; // Ej: "Creatina", "Whey Protein"

    @Column(name = "cantidad_g")
    private Double cantidadGramos;

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PerfilUsuario getPerfilUsuario() { return perfilUsuario; }
    public void setPerfilUsuario(PerfilUsuario perfilUsuario) { this.perfilUsuario = perfilUsuario; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getNombreSuplemento() { return nombreSuplemento; }
    public void setNombreSuplemento(String nombreSuplemento) { this.nombreSuplemento = nombreSuplemento; }
    public Double getCantidadGramos() { return cantidadGramos; }
    public void setCantidadGramos(Double cantidadGramos) { this.cantidadGramos = cantidadGramos; }
}