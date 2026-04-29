package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.PortafolioEstrategia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortafolioEstrategiaRepository extends JpaRepository<PortafolioEstrategia, Long> {
}
