package com.sebatrox.orquestador.repository;

import com.sebatrox.orquestador.entity.Fragmento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FragmentoRepository extends JpaRepository<Fragmento, Long> {

    // El truco: Pedimos todas las columnas, pero enviamos NULL en el lugar del embedding
    // para que Java no tenga que intentar convertir el tipo 'vector' a 'float[]'
    @Query(value = "SELECT id, contenido, documento_id, NULL as embedding " +
                   "FROM documento_fragmentos " +
                   "ORDER BY embedding <=> cast(:embedding as vector) " +
                   "LIMIT :limite", nativeQuery = true)
    List<Fragmento> buscarSimilares(@Param("embedding") float[] embedding, 
                                    @Param("limite") int limite);
}