package com.miDiario.blog.repository;

import com.miDiario.blog.model.Reaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReaccionRepository extends JpaRepository<Reaccion, Long> {
    long countByPublicacionId(Long publicacionId);
    Optional<Reaccion> findByPublicacionIdAndUsuarioId(Long publicacionId, Long usuarioId);
}