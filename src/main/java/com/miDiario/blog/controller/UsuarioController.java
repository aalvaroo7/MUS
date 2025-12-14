package com.miDiario.blog.controller;

import com.miDiario.blog.model.Usuario;
import com.miDiario.blog.service.UsuarioService;
import com.miDiario.blog.repository.UsuarioRepository; // Importante para buscar amigos
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set; // Necesario para la lista de amigos

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository; // Añadimos esto para leer amigos directamente

    // Constructor actualizado inyectando ambos
    public UsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    // ============================================================
    // NUEVO: OBTENER LISTA DE AMIGOS (PARA EL CHAT)
    // ============================================================
    @GetMapping("/{id}/amigos")
    public ResponseEntity<Set<Usuario>> obtenerAmigos(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(usuario -> ResponseEntity.ok(usuario.getAmigos()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ============================================================
    // ACTUALIZAR PERFIL
    // ============================================================
    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable Long id,
            @RequestBody Usuario usuarioDatos,
            HttpSession session) {

        Long usuarioSesionId = (Long) session.getAttribute("usuarioId");

        // Nota: Si usas Spring Security con 'permitAll', la sesión puede ser null a veces.
        // Asegúrate de gestionar esto en el frontend enviando el ID si es necesario,
        // o confiando en la sesión si el login la crea correctamente.
        if (usuarioSesionId == null) {
            // Intenta ver si el ID coincide aunque no haya sesión (para pruebas locales)
            // O devuelve error estricto:
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