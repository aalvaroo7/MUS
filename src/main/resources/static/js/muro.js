let usuarioLogueado = null;
let amigoActualId = null;
let pollingInterval = null;

document.addEventListener("DOMContentLoaded", () => {
    const usuarioLocal = localStorage.getItem("usuario");
    if (!usuarioLocal) {
        window.location.href = "/index.html";
        return;
    }
    usuarioLogueado = JSON.parse(usuarioLocal);

    cargarUsuarioEnHeader();
    cargarPublicaciones();
    iniciarSistemaChat(); // <--- INICIA EL CHAT

    const publicarBtn = document.getElementById("publicarBtn");
    if (publicarBtn) publicarBtn.addEventListener("click", publicar);

    const logoutBtn = document.getElementById("logout");
    if (logoutBtn) logoutBtn.addEventListener("click", cerrarSesion);

    const perfilBtn = document.getElementById("perfilBtn");
    if (perfilBtn) perfilBtn.addEventListener("click", () => window.location.href = "/html/perfil.html");
});

// --- FUNCIONES DEL MURO ---
function cargarUsuarioEnHeader() {
    if (usuarioLogueado) {
        const nombreEl = document.getElementById("nombreUsuario");
        if (nombreEl) nombreEl.textContent = usuarioLogueado.nombreUsuario || "Usuario";
    }
}

async function publicar() {
    const textoInput = document.getElementById("postTexto");
    const imagenInput = document.getElementById("postImagen");
    const contenido = textoInput.value;
    const archivo = imagenInput.files[0];

    if (!contenido && !archivo) {
        alert("Escribe algo o selecciona una imagen.");
        return;
    }

    const formData = new FormData();
    formData.append("contenido", contenido);
    // IMPORTANTE: Enviamos el ID del usuario si el backend lo necesita explícitamente,
    // aunque idealmente lo coge de la sesión. Lo añado por si acaso.
    formData.append("usuarioId", usuarioLogueado.id);
    if (archivo) formData.append("archivo", archivo);

    try {
        const response = await fetch('/api/publicaciones/crear', { method: 'POST', body: formData });
        if (response.ok) {
            textoInput.value = "";
            imagenInput.value = "";
            cargarPublicaciones();
        } else {
            alert("Error al publicar.");
        }
    } catch (error) {
        console.error("Error:", error);
    }
}

async function cargarPublicaciones() {
    try {
        const response = await fetch('/api/publicaciones/todas');
        if (!response.ok) return;
        const publicaciones = await response.json();
        const feed = document.getElementById("feed");
        feed.innerHTML = "";

        publicaciones.sort((a, b) => new Date(b.fechaPublicacion) - new Date(a.fechaPublicacion));

        if (publicaciones.length === 0) {
            feed.innerHTML = "<p style='text-align:center; padding:20px; color:#777;'>No hay publicaciones.</p>";
            return;
        }

        publicaciones.forEach(pub => {
            const postDiv = document.createElement("div");
            postDiv.className = "post";

            // Avatar
            let avatarUrl = pub.usuario && pub.usuario.fotoPerfil ? pub.usuario.fotoPerfil : `https://ui-avatars.com/api/?name=${pub.usuario ? pub.usuario.nombreUsuario : "U"}&background=random`;

            // Botón borrar
            let botonBorrar = (pub.usuario && pub.usuario.id === usuarioLogueado.id)
                ? `<button onclick="borrarPublicacion(${pub.id})" style="border:none; background:none; cursor:pointer;">🗑️</button>`
                : "";

            // Imagen post
            let imgHtml = pub.imagenUrl ? `<img src="${pub.imagenUrl}" style="max-width:100%; margin-top:10px; border-radius:5px;">` : "";

            postDiv.innerHTML = `
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
                    <div style="display:flex; align-items:center; gap:10px;">
                        <img src="${avatarUrl}" style="width:40px; height:40px; border-radius:50%; object-fit:cover;">
                        <strong>${pub.usuario ? pub.usuario.nombreUsuario : "Anónimo"}</strong>
                    </div>
                    ${botonBorrar}
                </div>
                <p>${pub.contenido}</p>
                ${imgHtml}
                <hr style="margin-top:15px; border-top:1px solid #eee;">
            `;
            feed.appendChild(postDiv);
        });
    } catch (e) { console.error(e); }
}

async function borrarPublicacion(id) {
    if(confirm("¿Borrar publicación?")) {
        await fetch(`/api/publicaciones/eliminar/${id}`, { method: 'DELETE' });
        cargarPublicaciones();
    }
}

function cerrarSesion(e) {
    if(e) e.preventDefault();
    localStorage.removeItem("usuario");
    if(pollingInterval) clearInterval(pollingInterval);
    fetch('/api/usuarios/logout', { method: 'POST' }).finally(() => window.location.href = "/html/login.html");
}

// --- FUNCIONES DEL CHAT (NUEVO) ---

function iniciarSistemaChat() {
    document.getElementById("chatBtn").addEventListener("click", () => {
        const ventana = document.getElementById("ventanaChat");
        ventana.style.display = (ventana.style.display === "none" || !ventana.style.display) ? "flex" : "none";
        if(ventana.style.display === "flex") cargarAmigosChat();
    });

    document.getElementById("btnEnviarMsg").addEventListener("click", enviarMensajeChat);

    // Buscar mensajes nuevos cada 3 segs
    actualizarNotificaciones();
    pollingInterval = setInterval(actualizarNotificaciones, 3000);
}

function actualizarNotificaciones() {
    fetch(`/api/chat/notificaciones?miId=${usuarioLogueado.id}`)
        .then(res => res.json())
        .then(num => {
            const badge = document.getElementById("notificacionBadge");
            if(num > 0) {
                badge.style.display = "block";
                badge.innerText = num;
            } else {
                badge.style.display = "none";
            }
        }).catch(e => {});
}

function cargarAmigosChat() {
    const lista = document.getElementById("listaAmigosChat");
    lista.innerHTML = "<p style='padding:10px; text-align:center'>Cargando...</p>";

    fetch(`/api/usuarios/${usuarioLogueado.id}/amigos`)
        .then(res => res.ok ? res.json() : [])
        .then(amigos => {
            lista.innerHTML = "";
            if(amigos.length === 0) {
                lista.innerHTML = "<p style='padding:20px; text-align:center'>No tienes amigos agregados.</p>";
                return;
            }
            amigos.forEach(amigo => {
                const div = document.createElement("div");
                div.className = "friend-item";
                div.innerHTML = `<span>👤 ${amigo.nombreUsuario}</span>`;
                div.onclick = () => abrirConversacion(amigo.id, amigo.nombreUsuario);
                lista.appendChild(div);
            });
        });
}

function abrirConversacion(idAmigo, nombre) {
    amigoActualId = idAmigo;
    document.getElementById("chatTitulo").innerText = nombre;
    document.getElementById("listaAmigosChat").style.display = "none";
    document.getElementById("zonaConversacion").style.display = "flex";
    cargarHistorial();
}

function volverALista() {
    amigoActualId = null;
    document.getElementById("chatTitulo").innerText = "Mis Amigos";
    document.getElementById("zonaConversacion").style.display = "none";
    document.getElementById("listaAmigosChat").style.display = "block";
    cargarAmigosChat();
}

function cargarHistorial() {
    if(!amigoActualId) return;
    const container = document.getElementById("mensajesContainer");

    fetch(`/api/chat/historial?miId=${usuarioLogueado.id}&amigoId=${amigoActualId}`)
        .then(res => res.json())
        .then(msgs => {
            container.innerHTML = "";
            msgs.forEach(m => {
                const div = document.createElement("div");
                div.className = "msg-bubble " + (m.emisor.id === usuarioLogueado.id ? "msg-mio" : "msg-otro");
                div.innerText = m.contenido;
                container.appendChild(div);
            });
            container.scrollTop = container.scrollHeight;
        });
}

function enviarMensajeChat() {
    const input = document.getElementById("inputMensaje");
    const txt = input.value.trim();
    if(!txt || !amigoActualId) return;

    fetch(`/api/chat/enviar?emisorId=${usuarioLogueado.id}&receptorId=${amigoActualId}&contenido=${encodeURIComponent(txt)}`, { method: "POST" })
        .then(res => {
            if(res.ok) {
                input.value = "";
                cargarHistorial();
            }
        });
}