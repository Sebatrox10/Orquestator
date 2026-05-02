package com.sebatrox.orquestador.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_hidratacion")
public class RegistroHidratacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilUsuario perfilUsuario;

    private LocalDateTime fechaHora;

    @Column(name = "cantidad_ml")
    private Integer cantidadMl;

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PerfilUsuario getPerfilUsuario() { return perfilUsuario; }
    public void setPerfilUsuario(PerfilUsuario perfilUsuario) { this.perfilUsuario = perfilUsuario; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getCantidadMl() { return cantidadMl; }
    public void setCantidadMl(Integer cantidadMl) { this.cantidadMl = cantidadMl;}
}