package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    // Al extender de JpaRepository, ya tenemos save(), findById(), etc.
}