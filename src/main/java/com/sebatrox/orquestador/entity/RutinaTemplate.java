package com.sebatrox.orquestador.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "rutina_templates")
public class RutinaTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre; // Ej: "Hipertrofia Piernas - Martes"
    private String descripcion;
    private String tipo; // "FUERZA", "CALISTENIA", "CARDIO"
    
    @OneToMany(mappedBy = "rutinaTemplate", cascade = CascadeType.ALL)
    private List<EjercicioTemplate> ejercicios;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    public List<EjercicioTemplate> getEjercicios() {
        return ejercicios;
    }
    public void setEjercicios(List<EjercicioTemplate> ejercicios) {
        this.ejercicios = ejercicios;
    }

    
}
