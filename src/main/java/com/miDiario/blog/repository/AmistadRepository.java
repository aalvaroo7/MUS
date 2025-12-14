package com.miDiario.blog.repository;

import com.miDiario.blog.model.Amistad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmistadRepository extends JpaRepository<Amistad, Long> {

    // Verificar si ya existe solicitud (en cualquier sentido)
    @Query("SELECT a FROM Amistad a WHERE " +
            "(a.solicitante.id = :id1 AND a.receptor.id = :id2) OR " +
            "(a.solicitante.id = :id2 AND a.receptor.id = :id1)")
    Optional<Amistad> buscarRelacion(Long id1, Long id2);

    // Listar SOLICITUDES PENDIENTES QUE RECIBÍ
    List<Amistad> findByReceptorIdAndEstado(Long receptorId, Amistad.EstadoAmistad estado);

    // Listar SOLICITUDES PENDIENTES QUE ENVIÉ
    List<Amistad> findBySolicitanteIdAndEstado(Long solicitanteId, Amistad.EstadoAmistad estado);

    // Listar AMIGOS CONFIRMADOS (Ya sea que yo envié o recibí)
    @Query("SELECT a FROM Amistad a WHERE " +
            "(a.solicitante.id = :miId OR a.receptor.id = :miId) " +
            "AND a.estado = 'ACEPTADA'")
    List<Amistad> misAmigos(Long miId);

    long countByReceptorIdAndVistaFalse(Long receptorId);
}
