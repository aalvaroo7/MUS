let usuarioLogueado = null;

document.addEventListener("DOMContentLoaded", () => {
    const usuarioLocal = localStorage.getItem("usuario");
    if (!usuarioLocal) {
        window.location.href = "/index.html";
        return;
    }
    usuarioLogueado = JSON.parse(usuarioLocal);

    cargarDatosPerfil();
    cargarAmigos();
    cargarSolicitudes();

    document.getElementById("volverBtn").addEventListener("click", () => window.location.href = "/html/muro.html");
    document.getElementById("perfilForm").addEventListener("submit", guardarCambios);

    document.getElementById("fileInput").addEventListener("change", function(e) {
        if (e.target.files && e.target.files[0]) {
            const reader = new FileReader();
            reader.onload = function(event) { document.getElementById("avatarImg").src = event.target.result; };
            reader.readAsDataURL(e.target.files[0]);
        }
    });

    document.getElementById("nombre").addEventListener("input", (e) => {
        document.getElementById("headerNombre").textContent = e.target.value;
    });

    document.getElementById("buscadorUsuarios").addEventListener("input", (e) => {
        const query = e.target.value;
        if(query.length > 2) buscarUsuarios(query);
    });
});

function mostrarTab(tabName) {
    document.getElementById("tab-amigos").style.display = "none";
    document.getElementById("tab-solicitudes").style.display = "none";
    document.getElementById("tab-buscar").style.display = "none";

    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    document.getElementById("tab-" + tabName).style.display = "block";

    const botones = document.querySelectorAll(".tab-btn");
    if(tabName === 'amigos') botones[0].classList.add("active");
    if(tabName === 'solicitudes') botones[1].classList.add("active");
    if(tabName === 'buscar') botones[2].classList.add("active");
}

async function cargarAmigos() {
    const lista = document.getElementById("listaAmigos");
    try {
        const res = await fetch(`/api/amistad/mis-amigos/${usuarioLogueado.id}`);
        const amigos = await res.json();

        lista.innerHTML = "";
        if(amigos.length === 0) {
            lista.innerHTML = "<p style='color:#777; padding:10px;'>Aún no tienes amigos agregados.</p>";
            return;
        }

        amigos.forEach(u => {
            const div = document.createElement("div");
            div.className = "user-item";
            let avatar = u.fotoPerfil || `https://ui-avatars.com/api/?name=${u.nombreUsuario}&background=random`;

            div.innerHTML = `
                <div class="user-info">
                    <img src="${avatar}" class="user-avatar-mini">
                    <div>
                        <strong>${u.nombreUsuario}</strong><br>
                        <small>${u.nombre}</small>
                    </div>
                </div>
                <button class="btn-mini btn-reject" onclick="eliminarAmigo(${u.id})">Eliminar</button>
            `;
            lista.appendChild(div);
        });
    } catch(e) { console.error(e); }
}

async function cargarSolicitudes() {
    try {
        const resRec = await fetch(`/api/amistad/solicitudes/recibidas/${usuarioLogueado.id}`);
        const recibidas = await resRec.json();
        const listaRec = document.getElementById("listaRecibidas");
        listaRec.innerHTML = "";

        const badge = document.getElementById("badgeSolicitudes");
        if(recibidas.length > 0) {
            badge.style.display = "inline-block";
            badge.innerText = recibidas.length;
        } else {
            badge.style.display = "none";
            listaRec.innerHTML = "<p style='color:#ccc; font-size:0.9em;'>No tienes invitaciones pendientes.</p>";
        }

        recibidas.forEach(sol => {
            const div = document.createElement("div");
            div.className = "user-item";
            const u = sol.solicitante;
            let avatar = u.fotoPerfil || `https://ui-avatars.com/api/?name=${u.nombreUsuario}&background=random`;

            div.innerHTML = `
                <div class="user-info">
                    <img src="${avatar}" class="user-avatar-mini">
                    <span>${u.nombreUsuario}</span>
                </div>
                <div>
                    <button class="btn-mini btn-accept" onclick="aceptarSolicitud(${sol.id})">✔</button>
                    <button class="btn-mini btn-reject" onclick="eliminarSolicitud(${sol.id})">✖</button>
                </div>
            `;
            listaRec.appendChild(div);
        });

        const resEnv = await fetch(`/api/amistad/solicitudes/enviadas/${usuarioLogueado.id}`);
        const enviadas = await resEnv.json();
        const listaEnv = document.getElementById("listaEnviadas");
        listaEnv.innerHTML = "";

        if(enviadas.length === 0) listaEnv.innerHTML = "<p style='color:#ccc; font-size:0.9em;'>No has enviado ninguna.</p>";

        enviadas.forEach(sol => {
            const div = document.createElement("div");
            div.className = "user-item";
            const u = sol.receptor;
            let avatar = u.fotoPerfil || `https://ui-avatars.com/api/?name=${u.nombreUsuario}&background=random`;

            div.innerHTML = `
                <div class="user-info">
                    <img src="${avatar}" class="user-avatar-mini" style="opacity:0.6">
                    <span style="color:#777">${u.nombreUsuario}</span>
                </div>
                <button class="btn-mini btn-pending">Pendiente</button>
            `;
            listaEnv.appendChild(div);
        });

    } catch(e) { console.error(e); }
}

async function buscarUsuarios(query) {
    const container = document.getElementById("resultadosBusqueda");
    container.innerHTML = "<p>Buscando...</p>";

    try {
        const res = await fetch(`/api/amistad/buscar?query=${query}&miId=${usuarioLogueado.id}`);
        const usuarios = await res.json();

        container.innerHTML = "";
        if(usuarios.length === 0) {
            container.innerHTML = "<p>No se encontraron usuarios.</p>";
            return;
        }

        usuarios.forEach(u => {
            const div = document.createElement("div");
            div.className = "user-item";
            let avatar = u.fotoPerfil || `https://ui-avatars.com/api/?name=${u.nombreUsuario}&background=random`;

            div.innerHTML = `
                <div class="user-info">
                    <img src="${avatar}" class="user-avatar-mini">
                    <strong>${u.nombreUsuario}</strong>
                </div>
                <button class="btn-mini btn-add" onclick="enviarSolicitud(${u.id})">Enviar Solicitud</button>
            `;
            container.appendChild(div);
        });
    } catch(e) { console.error(e); }
}

async function enviarSolicitud(receptorId) {
    if(!confirm("¿Enviar solicitud de amistad?")) return;
    await fetch(`/api/amistad/enviar?solicitanteId=${usuarioLogueado.id}&receptorId=${receptorId}`, { method: 'POST' });
    alert("Solicitud enviada");
    cargarSolicitudes();
    document.getElementById("buscadorUsuarios").value = "";
    document.getElementById("resultadosBusqueda").innerHTML = "";
}

async function aceptarSolicitud(idAmistad) {
    await fetch(`/api/amistad/aceptar/${idAmistad}`, { method: 'POST' });
    cargarSolicitudes();
    cargarAmigos();
}

async function eliminarSolicitud(idAmistad) {
    if(!confirm("¿Rechazar solicitud?")) return;
    await fetch(`/api/amistad/eliminar/${idAmistad}`, { method: 'DELETE' });
    cargarSolicitudes();
}

// NUEVO: ELIMINAR AMIGO CORRECTAMENTE
async function eliminarAmigo(idAmigoUsuario) {
    if (!confirm("¿Seguro que quieres eliminar a este amigo?")) return;

    try {
        const res = await fetch(`/api/amistad/eliminar-relacion?id1=${usuarioLogueado.id}&id2=${idAmigoUsuario}`, {
            method: 'DELETE'
        });

        if (res.ok) {
            alert("Amigo eliminado.");
            cargarAmigos();
        } else {
            alert("Error al eliminar.");
        }
    } catch (e) {
        console.error(e);
        alert("Error de conexión");
    }
}

// FUNCIONES PERFIL
function cargarDatosPerfil() {
    document.getElementById("headerNombre").textContent = usuarioLogueado.nombre;
    document.getElementById("nombre").value = usuarioLogueado.nombre;
    document.getElementById("genero").value = usuarioLogueado.genero || "No especificado";
    document.getElementById("nombreUsuario").value = usuarioLogueado.nombreUsuario;
    document.getElementById("email").value = usuarioLogueado.email || usuarioLogueado.correo;

    if (usuarioLogueado.fotoPerfil) {
        document.getElementById("avatarImg").src = usuarioLogueado.fotoPerfil;
    } else {
        document.getElementById("avatarImg").src = `https://ui-avatars.com/api/?name=${usuarioLogueado.nombre}&background=random&size=128`;
    }
}

const convertBase64 = (file) => {
    return new Promise((resolve, reject) => {
        const fileReader = new FileReader();
        fileReader.readAsDataURL(file);
        fileReader.onload = () => resolve(fileReader.result);
        fileReader.onerror = (error) => reject(error);
    });
};

async function guardarCambios(e) {
    e.preventDefault();
    const nuevoNombre = document.getElementById("nombre").value;
    const nuevoNick = document.getElementById("nombreUsuario").value;
    const nuevoEmail = document.getElementById("email").value;

    const fileInput = document.getElementById("fileInput");
    let fotoFinal = usuarioLogueado.fotoPerfil;

    if (fileInput.files.length > 0) {
        fotoFinal = await convertBase64(fileInput.files[0]);
    }

    const datosActualizar = {
        nombre: nuevoNombre,
        nombreUsuario: nuevoNick,
        email: nuevoEmail,
        fotoPerfil: fotoFinal
    };

    try {
        const response = await fetch(`/api/usuarios/actualizar/${usuarioLogueado.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datosActualizar)
        });

        if (response.ok) {
            const usuarioActualizado = await response.json();
            localStorage.setItem("usuario", JSON.stringify(usuarioActualizado));
            alert("¡Perfil actualizado!");
        } else {
            alert("Error al guardar.");
        }
    } catch (error) { console.error(error); }
}