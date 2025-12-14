package com.miDiario.blog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String apellidos;

    @Column(name = "nombre_usuario")
    private String nombreUsuario;

    @Column
    private String genero;

    @Column(name = "correo", nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "foto_perfil", columnDefinition = "LONGTEXT")
    private String fotoPerfil;

    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;

    // --- MENSAJES RECIBIDOS (Para el chat) ---
    // Mantenemos esto para que Hibernate sepa gestionar los mensajes borrados si borras un usuario
    @OneToMany(mappedBy = "receptor", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Mensaje> mensajesRecibidos;

    // NOTA: Hemos eliminado 'Set<Usuario> amigos' porque ahora la amistad
    // se gestiona a través de la entidad 'Amistad' y el 'AmistadRepository'.
}