package com.miDiario.blog.controller;

import com.miDiario.blog.model.Comentario;
import com.miDiario.blog.service.PublicacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crearPublicacion(@RequestParam("contenido") String contenido, @RequestParam(value = "archivo", required = false) MultipartFile archivo, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return ResponseEntity.status(401).body("Sesión expirada.");
        try {
            return ResponseEntity.ok(publicacionService.crearPublicacion(contenido, archivo, usuarioId));
        } catch (IOException e) { return ResponseEntity.internalServerError().body("Error imagen."); }
    }

    // AHORA DEVUELVE DTOs CON LA INFO DE LIKES
    @GetMapping("/todas")
    public ResponseEntity<?> obtenerTodas(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return ResponseEntity.status(401).body("No autenticado");

        return ResponseEntity.ok(publicacionService.obtenerTodasConInfo(usuarioId));
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return ResponseEntity.status(401).build();
        return publicacionService.eliminarPublicacion(id, usuarioId);
    }

    // --- NUEVOS ENDPOINTS ---

    @PostMapping("/like/{id}")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return ResponseEntity.status(401).build();

        boolean liked = publicacionService.toggleLike(id, usuarioId);
        return ResponseEntity.ok(liked); // Devuelve true si ahora tiene like, false si se quitó
    }

    @PostMapping("/comentar/{id}")
    public ResponseEntity<?> comentar(@PathVariable Long id, @RequestParam String texto, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return ResponseEntity.status(401).build();

        publicacionService.comentar(id, usuarioId, texto);
        return ResponseEntity.ok("Comentario guardado");
    }

    @GetMapping("/comentarios/{id}")
    public ResponseEntity<List<Comentario>> obtenerComentarios(@PathVariable Long id) {
        return ResponseEntity.ok(publicacionService.obtenerComentarios(id));
    }
}