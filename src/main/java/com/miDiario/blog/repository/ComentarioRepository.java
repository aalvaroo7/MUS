package com.miDiario.blog.repository;

import com.miDiario.blog.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByPublicacionIdOrderByFechaDesc(Long publicacionId);
    long countByPublicacionId(Long publicacionId);
}