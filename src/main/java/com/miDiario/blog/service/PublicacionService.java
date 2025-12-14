package com.miDiario.blog.service;

import com.miDiario.blog.dto.PublicacionResponseDTO;
import com.miDiario.blog.model.*;
import com.miDiario.blog.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReaccionRepository reaccionRepository;
    private final ComentarioRepository comentarioRepository;

    public PublicacionService(PublicacionRepository publicacionRepository,
                              UsuarioRepository usuarioRepository,
                              ReaccionRepository reaccionRepository,
                              ComentarioRepository comentarioRepository) {
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.reaccionRepository = reaccionRepository;
        this.comentarioRepository = comentarioRepository;
    }

    // CREAR PUBLICACIÓN
    public Publicacion crearPublicacion(String contenido, MultipartFile archivo, Long usuarioId) throws IOException {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Publicacion nueva = new Publicacion();
        nueva.setContenido(contenido);
        nueva.setUsuario(usuario);
        nueva.setFechaPublicacion(LocalDateTime.now());

        if (archivo != null && !archivo.isEmpty()) {
            byte[] bytes = archivo.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            nueva.setImagenUrl("data:" + archivo.getContentType() + ";base64," + base64Image);
        }
        return publicacionRepository.save(nueva);
    }

    // OBTENER TODAS (CON DTO PARA LIKES Y COMENTARIOS)
    public List<PublicacionResponseDTO> obtenerTodasConInfo(Long miUsuarioId) {
        List<Publicacion> publicaciones = publicacionRepository.findAll(); // Idealmente añadir Sort por fecha
        List<PublicacionResponseDTO> resultado = new ArrayList<>();

        for (Publicacion p : publicaciones) {
            PublicacionResponseDTO dto = new PublicacionResponseDTO();
            dto.setId(p.getId());
            dto.setContenido(p.getContenido());
            dto.setImagenUrl(p.getImagenUrl());
            dto.setFechaPublicacion(p.getFechaPublicacion());
            dto.setUsuario(p.getUsuario());

            // Contar likes
            dto.setNumLikes(reaccionRepository.countByPublicacionId(p.getId()));

            // Contar comentarios
            dto.setNumComentarios(comentarioRepository.countByPublicacionId(p.getId()));

            // ¿Yo le di like?
            boolean liked = reaccionRepository.findByPublicacionIdAndUsuarioId(p.getId(), miUsuarioId).isPresent();
            dto.setLikedByMe(liked);

            resultado.add(dto);
        }
        return resultado;
    }

    // DAR / QUITAR LIKE
    public boolean toggleLike(Long publicacionId, Long usuarioId) {
        Optional<Reaccion> existente = reaccionRepository.findByPublicacionIdAndUsuarioId(publicacionId, usuarioId);

        if (existente.isPresent()) {
            // Si ya existe, quitamos el like
            reaccionRepository.delete(existente.get());
            return false; // Like quitado
        } else {
            // Si no existe, lo creamos
            Publicacion p = publicacionRepository.findById(publicacionId).orElseThrow();
            Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();

            Reaccion r = new Reaccion();
            r.setPublicacion(p);
            r.setUsuario(u);
            reaccionRepository.save(r);
            return true; // Like puesto
        }
    }

    // COMENTAR
    public Comentario comentar(Long publicacionId, Long usuarioId, String texto) {
        Publicacion p = publicacionRepository.findById(publicacionId).orElseThrow();
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();

        Comentario c = new Comentario();
        c.setPublicacion(p);
        c.setUsuario(u);
        c.setTexto(texto);
        c.setFecha(LocalDateTime.now());

        return comentarioRepository.save(c);
    }

    // OBTENER COMENTARIOS DE UN POST
    public List<Comentario> obtenerComentarios(Long publicacionId) {
        return comentarioRepository.findByPublicacionIdOrderByFechaDesc(publicacionId);
    }

    // ELIMINAR PUBLICACIÓN
    public ResponseEntity<?> eliminarPublicacion(Long idPublicacion, Long idUsuario) {
        Optional<Publicacion> pubOpt = publicacionRepository.findById(idPublicacion);
        if (pubOpt.isEmpty()) return ResponseEntity.status(404).body("No encontrada");

        if (!pubOpt.get().getUsuario().getId().equals(idUsuario)) {
            return ResponseEntity.status(403).body("No tienes permiso");
        }
        publicacionRepository.delete(pubOpt.get());
        return ResponseEntity.ok("Eliminada");
    }
}