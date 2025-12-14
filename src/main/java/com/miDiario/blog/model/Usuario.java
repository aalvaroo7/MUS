package com.miDiario.blog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
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

    // --- NUEVO: LISTA DE AMIGOS ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuarios_amigos",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "amigo_id")
    )
    @JsonIgnore
    private Set<Usuario> amigos = new HashSet<>();

    // --- NUEVO: MENSAJES RECIBIDOS ---
    @OneToMany(mappedBy = "receptor")
    @JsonIgnore
    private Set<Mensaje> mensajesRecibidos;
}