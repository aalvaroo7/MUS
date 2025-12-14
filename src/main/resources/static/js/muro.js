let usuarioLogueado = null;
let amigoActualId = null;
let pollingInterval = null;

document.addEventListener("DOMContentLoaded", () => {
    const usuarioLocal = localStorage.getItem("usuario");
    if (!usuarioLocal) { window.location.href = "/index.html"; return; }
    usuarioLogueado = JSON.parse(usuarioLocal);

    cargarUsuarioEnHeader();
    cargarPublicaciones();
    iniciarSistemaChat();

    const publicarBtn = document.getElementById("publicarBtn");
    if (publicarBtn) publicarBtn.addEventListener("click", publicar);
    const logoutBtn = document.getElementById("logout");
    if (logoutBtn) logoutBtn.addEventListener("click", cerrarSesion);
    const perfilBtn = document.getElementById("perfilBtn");
    if (perfilBtn) perfilBtn.addEventListener("click", () => window.location.href = "/html/perfil.html");
});

// ==========================================
// FUNCIONES DEL MURO
// ==========================================

function cargarUsuarioEnHeader() {
    if (usuarioLogueado) {
        document.getElementById("nombreUsuario").textContent = usuarioLogueado.nombreUsuario || "Usuario";
    }
}

async function publicar() {
    const textoInput = document.getElementById("postTexto");
    const imagenInput = document.getElementById("postImagen");
    const contenido = textoInput.value;
    const archivo = imagenInput.files[0];

    if (!contenido && !archivo) { alert("Escribe algo."); return; }

    const formData = new FormData();
    formData.append("contenido", contenido);
    formData.append("usuarioId", usuarioLogueado.id);
    if (archivo) formData.append("archivo", archivo);

    try {
        const response = await fetch('/api/publicaciones/crear', { method: 'POST', body: formData });
        if (response.ok) {
            textoInput.value = ""; imagenInput.value = ""; cargarPublicaciones();
        } else { alert("Error al publicar."); }
    } catch (e) { console.error(e); }
}

function calcularTiempoRelativo(fechaISO) {
    const ahora = new Date();
    const fecha = new Date(fechaISO);
    const diffSegundos = Math.floor((ahora - fecha) / 1000);

    if (diffSegundos < 60) return "Hace un momento";
    const diffMinutos = Math.floor(diffSegundos / 60);
    if (diffMinutos < 60) return `Hace ${diffMinutos} minuto${diffMinutos > 1 ? 's' : ''}`;
    const diffHoras = Math.floor(diffMinutos / 60);
    if (diffHoras < 24) return `Hace ${diffHoras} hora${diffHoras > 1 ? 's' : ''}`;
    const diffDias = Math.floor(diffHoras / 24);
    if (diffDias < 7) return `Hace ${diffDias} día${diffDias > 1 ? 's' : ''}`;
    const diffSemanas = Math.floor(diffDias / 7);
    return `Hace ${diffSemanas} semana${diffSemanas > 1 ? 's' : ''}`;
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

            let avatarUrl = pub.usuario && pub.usuario.fotoPerfil ? pub.usuario.fotoPerfil : `https://ui-avatars.com/api/?name=${pub.usuario ? pub.usuario.nombreUsuario : "U"}&background=random`;
            let botonBorrar = (pub.usuario && pub.usuario.id === usuarioLogueado.id)
                ? `<button onclick="borrarPublicacion(${pub.id})" class="borrar-btn">🗑️</button>` : "";
            let imgHtml = pub.imagenUrl ? `<img src="${pub.imagenUrl}" class="post-img">` : "";

            const claseLike = pub.likedByMe ? "liked" : "";
            const corazon = pub.likedByMe ? "❤️" : "🤍";
            const tiempoRelativo = calcularTiempoRelativo(pub.fechaPublicacion);

            postDiv.innerHTML = `
                <div class="post-header">
                    <div style="display:flex; align-items:center; gap:10px; cursor:pointer;"
                         onclick="verPerfilRapido(${pub.usuario.id}, '${pub.usuario.nombreUsuario}', '${avatarUrl}')">
                        <img src="${avatarUrl}" style="width:40px; height:40px; border-radius:50%; object-fit:cover;">
                        <div>
                            <strong>${pub.usuario.nombreUsuario}</strong>
                            <div style="font-size:0.8em; color:#666;">${tiempoRelativo}</div>
                        </div>
                    </div>
                    ${botonBorrar}
                </div>
                <p>${pub.contenido}</p>
                ${imgHtml}

                <div class="post-actions">
                    <button class="action-btn ${claseLike}" id="like-btn-${pub.id}" onclick="darLike(${pub.id})">
                        <span class="heart-icon">${corazon}</span> <span id="likes-count-${pub.id}">${pub.numLikes}</span>
                    </button>
                    <button class="action-btn" onclick="toggleComentarios(${pub.id})">
                        💬 <span id="coments-count-${pub.id}">${pub.numComentarios}</span>
                    </button>
                </div>
                <div id="comentarios-section-${pub.id}" class="comentarios-section">
                    <div class="input-comentario-wrapper">
                        <input type="text" id="input-comentario-${pub.id}" class="input-comentario" placeholder="Escribe un comentario...">
                        <button class="btn-publicar-comentario" onclick="enviarComentario(${pub.id})">Enviar</button>
                    </div>
                    <div id="lista-comentarios-${pub.id}" class="lista-comentarios"></div>
                </div>
            `;
            feed.appendChild(postDiv);
        });
    } catch (e) { console.error(e); }
}

async function darLike(id) {
    try {
        const btn = document.getElementById(`like-btn-${id}`);
        const count = document.getElementById(`likes-count-${id}`);
        const icon = btn.querySelector(".heart-icon");
        const liked = btn.classList.contains("liked");
        let n = parseInt(count.innerText);

        if (liked) { btn.classList.remove("liked"); icon.innerText="🤍"; count.innerText=Math.max(0, n-1); }
        else { btn.classList.add("liked"); icon.innerText="❤️"; count.innerText=n+1; }
        await fetch(`/api/publicaciones/like/${id}`, {method:'POST'});
    } catch(e) {}
}

async function toggleComentarios(id) {
    const s = document.getElementById(`comentarios-section-${id}`);
    if(s.style.display==="block") s.style.display="none";
    else { s.style.display="block"; cargarComentarios(id); }
}

async function cargarComentarios(id) {
    const l = document.getElementById(`lista-comentarios-${id}`);
    l.innerHTML = "<small>Cargando...</small>";
    const r = await fetch(`/api/publicaciones/comentarios/${id}`);
    const c = await r.json();
    l.innerHTML="";
    if(c.length===0) { l.innerHTML="<small>Sin comentarios.</small>"; return; }
    c.forEach(x=>{
        const d=document.createElement("div"); d.className="comentario-item";
        let a=x.usuario.fotoPerfil||`https://ui-avatars.com/api/?name=${x.usuario.nombreUsuario}&background=random&size=30`;
        d.innerHTML=`<img src="${a}" style="width:30px;height:30px;border-radius:50%;margin-top:5px;"><div class="comentario-burbuja"><strong>${x.usuario.nombreUsuario}</strong><p style="margin:0">${x.texto}</p></div>`;
        l.appendChild(d);
    });
}

async function enviarComentario(id) {
    const i=document.getElementById(`input-comentario-${id}`);
    const t=i.value.trim(); if(!t)return;
    const r=await fetch(`/api/publicaciones/comentar/${id}`,{method:'POST',body:new URLSearchParams({texto:t})});
    if(r.ok){ i.value=""; cargarComentarios(id); const c=document.getElementById(`coments-count-${id}`); c.innerText=parseInt(c.innerText)+1; }
}

async function borrarPublicacion(id) {
    if(confirm("¿Borrar?")) { await fetch(`/api/publicaciones/eliminar/${id}`,{method:'DELETE'}); cargarPublicaciones(); }
}

function cerrarSesion(e) {
    if(e)e.preventDefault(); localStorage.removeItem("usuario");
    if(pollingInterval) clearInterval(pollingInterval);
    fetch('/api/usuarios/logout',{method:'POST'}).finally(()=>window.location.href="/html/login.html");
}

// ==========================================
// SISTEMA DE CHAT
// ==========================================

function iniciarSistemaChat() {
    const chatBtn = document.getElementById("chatBtn");
    if (chatBtn) chatBtn.addEventListener("click", () => {
        const v = document.getElementById("ventanaChat");
        if(v.style.display === "flex") {
            v.style.display = "none";
        } else {
            v.style.display = "flex";
            cargarAmigosChat();
        }
    });
    document.getElementById("btnEnviarMsg").addEventListener("click", enviarMensajeChat);

    actualizarNotificaciones();
    pollingInterval = setInterval(actualizarNotificaciones, 3000);
}

function actualizarNotificaciones() {
    // Solo actualizamos el Badge Global (Botón Chats)
    fetch(`/api/chat/notificaciones?miId=${usuarioLogueado.id}`)
        .then(r=>r.json())
        .then(total => {
            const badge = document.getElementById("notificacionBadge");
            if(total > 0) {
                badge.style.display = "block";
                badge.innerText = total;
            } else {
                badge.style.display = "none";
            }
        }).catch(e=>{});
}

async function cargarAmigosChat() {
    const lista = document.getElementById("listaAmigosChat");
    lista.innerHTML = "<p style='padding:10px; text-align:center'>Cargando...</p>";

    try {
        const resAmigos = await fetch(`/api/usuarios/${usuarioLogueado.id}/amigos`);
        const amigos = await resAmigos.json();

        lista.innerHTML = "";
        if(amigos.length === 0) {
            lista.innerHTML = "<p style='padding:20px; text-align:center; color:#777;'>Aún no tienes amigos.</p>";
            return;
        }

        amigos.forEach(amigo => {
            const div = document.createElement("div");
            div.className = "friend-item";

            div.innerHTML = `
                <div class="friend-info">
                    <span>👤 ${amigo.nombreUsuario}</span>
                </div>
            `;

            div.onclick = () => abrirConversacion(amigo.id, amigo.nombreUsuario);
            lista.appendChild(div);
        });

    } catch (e) {
        console.error(e);
        lista.innerHTML = "<p style='color:red; text-align:center'>Error de conexión</p>";
    }
}

function abrirConversacion(id, nombre) {
    amigoActualId = id;
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
    const c = document.getElementById("mensajesContainer");
    fetch(`/api/chat/historial?miId=${usuarioLogueado.id}&amigoId=${amigoActualId}`).then(r=>r.json()).then(m=>{
        c.innerHTML = "";
        m.forEach(msg => {
            const d = document.createElement("div");
            d.className = "msg-bubble " + (msg.emisor.id === usuarioLogueado.id ? "msg-mio" : "msg-otro");
            d.innerText = msg.contenido;
            c.appendChild(d);
        });
        c.scrollTop = c.scrollHeight;
    });
}

function enviarMensajeChat() {
    const i = document.getElementById("inputMensaje"); const t = i.value.trim();
    if(!t || !amigoActualId) return;
    fetch(`/api/chat/enviar?emisorId=${usuarioLogueado.id}&receptorId=${amigoActualId}&contenido=${encodeURIComponent(t)}`, {method:"POST"})
        .then(r => { if(r.ok){ i.value=""; cargarHistorial(); } });
}

function verPerfilRapido(id, n, a) {
    if(id===usuarioLogueado.id){window.location.href="/html/perfil.html";return;}
    document.getElementById("modalAvatar").src = a; document.getElementById("modalNombre").innerText = n;
    const b = document.getElementById("btnAgregarMuro"), bn = b.cloneNode(true);
    b.parentNode.replaceChild(bn, b); bn.onclick = () => enviarSolicitudDesdeModal(id);
    document.getElementById("modalUsuario").style.display = "flex";
}

async function enviarSolicitudDesdeModal(id) {
    if(!confirm("¿Enviar solicitud?")) return;
    await fetch(`/api/amistad/enviar?solicitanteId=${usuarioLogueado.id}&receptorId=${id}`, {method:'POST'});
    alert("Enviada"); document.getElementById("modalUsuario").style.display = "none";
}