package com.miDiario.blog.controller;

import com.miDiario.blog.model.Amistad;
import com.miDiario.blog.model.Usuario;
import com.miDiario.blog.repository.AmistadRepository;
import com.miDiario.blog.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/amistad")
public class AmistadController {

    @Autowired private AmistadRepository amistadRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // 1. Enviar Solicitud
    @PostMapping("/enviar")
    public ResponseEntity<?> enviarSolicitud(@RequestParam Long solicitanteId, @RequestParam Long receptorId) {
        if (solicitanteId.equals(receptorId)) return ResponseEntity.badRequest().body("No puedes agregarte a ti mismo.");

        // Verificar si ya existe relación
        if (amistadRepository.buscarRelacion(solicitanteId, receptorId).isPresent()) {
            return ResponseEntity.badRequest().body("Ya existe una solicitud o amistad.");
        }

        Usuario solicitante = usuarioRepository.findById(solicitanteId).orElseThrow();
        Usuario receptor = usuarioRepository.findById(receptorId).orElseThrow();

        Amistad amistad = new Amistad();
        amistad.setSolicitante(solicitante);
        amistad.setReceptor(receptor);
        amistad.setEstado(Amistad.EstadoAmistad.PENDIENTE);
        amistad.setFecha(LocalDateTime.now());
        amistad.setVista(false);

        amistadRepository.save(amistad);
        return ResponseEntity.ok("Solicitud enviada");
    }

    // 2. Aceptar Solicitud
    @PostMapping("/aceptar/{idAmistad}")
    public ResponseEntity<?> aceptarSolicitud(@PathVariable Long idAmistad) {
        Amistad amistad = amistadRepository.findById(idAmistad).orElseThrow();
        amistad.setEstado(Amistad.EstadoAmistad.ACEPTADA);
        amistad.setVista(true);
        amistadRepository.save(amistad);
        return ResponseEntity.ok("Amistad aceptada");
    }

    // 3. Rechazar o Cancelar (Por ID de la solicitud)
    @DeleteMapping("/eliminar/{idAmistad}")
    public ResponseEntity<?> eliminarAmistad(@PathVariable Long idAmistad) {
        amistadRepository.deleteById(idAmistad);
        return ResponseEntity.ok("Eliminada");
    }

    // 4. NUEVO: Eliminar amistad sabiendo solo los dos usuarios (Para el botón de eliminar amigo)
    @DeleteMapping("/eliminar-relacion")
    public ResponseEntity<?> eliminarPorUsuarios(@RequestParam Long id1, @RequestParam Long id2) {
        return amistadRepository.buscarRelacion(id1, id2)
                .map(amistad -> {
                    amistadRepository.delete(amistad);
                    return ResponseEntity.ok("Amistad eliminada");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. Listar mis amigos
    @GetMapping("/mis-amigos/{miId}")
    public List<Usuario> listarAmigos(@PathVariable Long miId) {
        List<Amistad> amistades = amistadRepository.misAmigos(miId);
        List<Usuario> amigos = new ArrayList<>();

        for (Amistad a : amistades) {
            if (a.getSolicitante().getId().equals(miId)) {
                amigos.add(a.getReceptor());
            } else {
                amigos.add(a.getSolicitante());
            }
        }
        return amigos;
    }

    // 6. Listar Solicitudes Recibidas
    @GetMapping("/solicitudes/recibidas/{miId}")
    public List<Amistad> verSolicitudesRecibidas(@PathVariable Long miId) {
        return amistadRepository.findByReceptorIdAndEstado(miId, Amistad.EstadoAmistad.PENDIENTE);
    }

    // 7. Listar Solicitudes Enviadas
    @GetMapping("/solicitudes/enviadas/{miId}")
    public List<Amistad> verSolicitudesEnviadas(@PathVariable Long miId) {
        return amistadRepository.findBySolicitanteIdAndEstado(miId, Amistad.EstadoAmistad.PENDIENTE);
    }

    // 8. Notificaciones
    @GetMapping("/notificaciones/{miId}")
    public long contarNotificaciones(@PathVariable Long miId) {
        return amistadRepository.countByReceptorIdAndVistaFalse(miId);
    }

    // 9. Buscar usuarios
    @GetMapping("/buscar")
    public List<Usuario> buscarUsuarios(@RequestParam String query, @RequestParam Long miId) {
        List<Usuario> todos = usuarioRepository.findAll();
        List<Usuario> resultado = new ArrayList<>();

        for(Usuario u : todos) {
            if(u.getId().equals(miId)) continue;

            if(u.getNombreUsuario().toLowerCase().contains(query.toLowerCase()) ||
                    u.getNombre().toLowerCase().contains(query.toLowerCase())) {
                resultado.add(u);
            }
        }
        return resultado;
    }
}