package com.sebatrox.orquestador.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "investigacion_documentos")
@Data
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    
    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    private String estado; // PENDIENTE, PROCESADO, ERROR

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL)
    private List<Fragmento> fragmentos;
}