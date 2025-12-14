package com.miDiario.blog.controller;

import com.miDiario.blog.model.Amistad;
import com.miDiario.blog.model.Usuario;
import com.miDiario.blog.repository.AmistadRepository;
import com.miDiario.blog.repository.UsuarioRepository;
import com.miDiario.blog.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final AmistadRepository amistadRepository; // <--- NUEVA DEPENDENCIA

    // Constructor actualizado inyectando los 3 componentes
    public UsuarioController(UsuarioService usuarioService,
                             UsuarioRepository usuarioRepository,
                             AmistadRepository amistadRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.amistadRepository = amistadRepository;
    }

    // ============================================================
    // OBTENER LISTA DE AMIGOS (ACTUALIZADO PARA SOLICITUDES)
    // ============================================================
    // Este endpoint es el que usa el CHAT para saber con quién puedes hablar.
    // Ahora busca solo las amistades que estén en estado 'ACEPTADA'.
    @GetMapping("/{id}/amigos")
    public ResponseEntity<List<Usuario>> obtenerAmigos(@PathVariable Long id) {
        // 1. Buscamos todas las relaciones aceptadas donde participa el usuario
        List<Amistad> amistades = amistadRepository.misAmigos(id);

        List<Usuario> amigos = new ArrayList<>();

        // 2. Filtramos para obtener a "la otra persona"
        for (Amistad a : amistades) {
            // Si el ID coincide con el solicitante, el amigo es el receptor
            if (a.getSolicitante().getId().equals(id)) {
                amigos.add(a.getReceptor());
            }
            // Si no, el amigo es el solicitante
            else {
                amigos.add(a.getSolicitante());
            }
        }
        return ResponseEntity.ok(amigos);
    }

    // ============================================================
    // ACTUALIZAR PERFIL (Mantenemos tu lógica original)
    // ============================================================
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable Long id,
            @RequestBody Usuario usuarioDatos,
            HttpSession session) {

        Long usuarioSesionId = (Long) session.getAttribute("usuarioId");

        // Verificación de seguridad
        if (usuarioSesionId == null) {
            // Si estás probando sin login, puedes comentar este bloque,
            // pero para producción es necesario.
            return ResponseEntity.status(401).body("No has iniciado sesión.");
        }

        if (!usuarioSesionId.equals(id)) {
            return ResponseEntity.status(403).body("No tienes permiso para editar este perfil.");
        }

        try {
            Usuario usuarioActualizado = usuarioService.actualizar(id, usuarioDatos);
            return ResponseEntity.ok(usuarioActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al actualizar el perfil.");
        }
    }

    // ============================================================
    // CERRAR SESIÓN (LOGOUT)
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<?> cerrarSesion(HttpSession session) {
        usuarioService.logout(session);
        return ResponseEntity.ok("Sesión cerrada correctamente");
    }

    // ============================================================
    // BLOQUEAR USUARIO (SOLO ADMIN)
    // ============================================================
    @PutMapping("/bloquear/{id}")
    public ResponseEntity<?> bloquearUsuario(
            @PathVariable Long id,
            HttpSession session) {

        if (session.getAttribute("usuarioId") == null) {
            return ResponseEntity.status(401).body("No autenticado");
        }

        Long adminId = (Long) session.getAttribute("usuarioId");
        return usuarioService.bloquear(adminId, id);
    }
}