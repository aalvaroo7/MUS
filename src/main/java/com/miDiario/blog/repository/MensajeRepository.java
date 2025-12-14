package com.miDiario.blog.repository;

import com.miDiario.blog.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // 1. Obtener la conversación entre dos personas (A->B y B->A) ordenada por fecha
    @Query("SELECT m FROM Mensaje m WHERE (m.emisor.id = :usuario1 AND m.receptor.id = :usuario2) OR (m.emisor.id = :usuario2 AND m.receptor.id = :usuario1) ORDER BY m.fecha ASC")
    List<Mensaje> obtenerChat(Long usuario1, Long usuario2);

    // 2. Contar mensajes que NO he leído (para el icono rojo)
    long countByReceptorIdAndLeidoFalse(Long receptorId);

    // 3. Buscar mensajes no leídos de un amigo concreto (para marcarlos como leídos al abrir chat)
    List<Mensaje> findByEmisorIdAndReceptorIdAndLeidoFalse(Long emisorId, Long receptorId);
}
