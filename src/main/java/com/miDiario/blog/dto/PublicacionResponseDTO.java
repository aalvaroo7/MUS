package com.miDiario.blog.dto;

import com.miDiario.blog.model.Usuario;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class PublicacionResponseDTO {
    private Long id;
    private String contenido;
    private String imagenUrl;
    private LocalDateTime fechaPublicacion;
    private Usuario usuario; // El autor

    // NUEVOS DATOS CALCULADOS
    private long numLikes;
    private long numComentarios;
    private boolean likedByMe; // ¿Yo le di like? (Para poner el corazón rojo)
}