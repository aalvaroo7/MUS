package com.miDiario.blog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "reacciones", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"publicacion_id", "usuario_id"})
})
@Getter @Setter @NoArgsConstructor
public class Reaccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private String tipo = "LIKE"; // Por defecto
    private LocalDateTime fecha = LocalDateTime.now();
}