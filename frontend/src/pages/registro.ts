import { API_BASE_URL } from "../data/apiClient";
import { ejecutarConBoton } from "../components/botonAccion";
import { mensajeError } from "../components/mensajes";
import { destinoTrasLogin } from "../utils/auth";
import { guardarSesion, obtenerSesion, type Sesion } from "../utils/session";
import { encabezado } from "../components/encabezado";

const app = document.querySelector<HTMLDivElement>("#app")!;
const next = new URLSearchParams(window.location.search).get("next");

function render(): void {
    const linkLogin = next ? `/login.html?next=${encodeURIComponent(next)}` : "/login.html";
    app.innerHTML = `
    ${encabezado()}
    <main class="login">
      <a class="volver" href="/">&larr; Volver al inicio</a>
      <h1>Crear cuenta</h1>

      <form id="form-registro" novalidate>
        <label for="nombre">Nombre y apellido</label>
        <input type="text" id="nombre" name="nombre" required maxlength="150" autocomplete="name" />

        <label for="email">Email</label>
        <input type="email" id="email" name="email" required maxlength="150" autocomplete="username" />

        <label for="password">Contraseña (mínimo 8 caracteres)</label>
        <input type="password" id="password" name="password" required minlength="8" maxlength="72" autocomplete="new-password" />

        <label for="direccion">Dirección de entrega</label>
        <input type="text" id="direccion" name="direccion" required maxlength="255" autocomplete="street-address" />

        <button type="submit">Registrarme</button>
      </form>

      <p>¿Ya tenés cuenta? <a href="${linkLogin}">Ingresá</a></p>
      <div id="mensaje" aria-live="polite"></div>
    </main>
  `;
}

// Recibe el HTML que arma el componente (mensajeError).
function mostrarMensaje(html: string): void {
    document.querySelector<HTMLElement>("#mensaje")!.innerHTML = html;
}

async function manejarEnvio(event: SubmitEvent): Promise<void> {
    event.preventDefault();

    const form = event.currentTarget as HTMLFormElement;
    const boton = form.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    const nombre = form.querySelector<HTMLInputElement>("#nombre")!.value.trim();
    const email = form.querySelector<HTMLInputElement>("#email")!.value.trim();
    const password = form.querySelector<HTMLInputElement>("#password")!.value;
    const direccion = form.querySelector<HTMLInputElement>("#direccion")!.value.trim();

    if (!nombre || !email || !password || !direccion) {
        mostrarMensaje(mensajeError("Completá todos los campos."));
        return;
    }
    if (password.length < 8) {
        mostrarMensaje(mensajeError("La contraseña tiene que tener al menos 8 caracteres."));
        return;
    }

    mostrarMensaje("");

    await ejecutarConBoton(
        boton,
        "Creando tu cuenta...",
        async () => {
            const response = await fetch(`${API_BASE_URL}/auth/registro`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ nombre, email, password, direccion }),
            });

            if (!response.ok) {
                let detalle = "No se pudo completar el registro.";
                try {
                    const error = await response.json();
                    detalle = error.message || detalle;
                } catch {
                    // Respuesta sin cuerpo JSON: se usa el mensaje generico.
                }
                throw new Error(detalle);
            }

            const sesion: Sesion = await response.json();
            guardarSesion(sesion);
            window.location.href = "/" + destinoTrasLogin(sesion.rol, next);
        },
        undefined, // se muestra el mensaje del backend
        (mensaje) => mostrarMensaje(mensajeError(mensaje)),
    );
}

function init(): void {
    const sesionActual = obtenerSesion();
    if (sesionActual) {
        window.location.href = "/" + destinoTrasLogin(sesionActual.rol, next);
        return;
    }
    render();
    document.querySelector<HTMLFormElement>("#form-registro")!.addEventListener("submit", manejarEnvio);
}

init();