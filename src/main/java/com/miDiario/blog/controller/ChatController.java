package com.miDiario.blog.controller;

import com.miDiario.blog.model.Mensaje;
import com.miDiario.blog.model.Usuario;
import com.miDiario.blog.repository.MensajeRepository;
import com.miDiario.blog.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private MensajeRepository mensajeRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Enviar un mensaje nuevo
    @PostMapping("/enviar")
    public String enviarMensaje(@RequestParam Long emisorId, @RequestParam Long receptorId, @RequestParam String contenido) {
        Usuario emisor = usuarioRepository.findById(emisorId).orElseThrow();
        Usuario receptor = usuarioRepository.findById(receptorId).orElseThrow();

        Mensaje mensaje = new Mensaje();
        mensaje.setEmisor(emisor);
        mensaje.setReceptor(receptor);
        mensaje.setContenido(contenido);
        mensaje.setFecha(LocalDateTime.now());
        mensaje.setLeido(false);

        mensajeRepository.save(mensaje);
        return "Mensaje enviado";
    }

    // Obtener historial y marcar como LEÍDOS automáticamente
    @GetMapping("/historial")
    public List<Mensaje> obtenerHistorial(@RequestParam Long miId, @RequestParam Long amigoId) {
        // 1. Buscamos los mensajes que ese amigo me envió y no he leído
        List<Mensaje> noLeidos = mensajeRepository.findByEmisorIdAndReceptorIdAndLeidoFalse(amigoId, miId);

        // 2. Los marcamos como leídos
        if (!noLeidos.isEmpty()) {
            noLeidos.forEach(m -> m.setLeido(true));
            mensajeRepository.saveAll(noLeidos);
        }

        // 3. Devolvemos toda la conversación
        return mensajeRepository.obtenerChat(miId, amigoId);
    }

    // Número de mensajes totales sin leer (para el badge rojo)
    @GetMapping("/notificaciones")
    public long contarNoLeidos(@RequestParam Long miId) {
        return mensajeRepository.countByReceptorIdAndLeidoFalse(miId);
    }
}