package com.sebatrox.orquestador.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "documento_fragmentos")
@Data
public class Fragmento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    // Con esto le decimos a Hibernate: "Trata esto como un objeto binario de pgvector"
    @Column(columnDefinition = "vector(384)")
    private float[] embedding;
}