import { API_BASE_URL } from "../data/apiClient";
import { ejecutarConBoton } from "../components/botonAccion";
import { mensajeError } from "../components/mensajes";
import { destinoTrasLogin } from "../utils/auth";
import { guardarSesion, obtenerSesion, type Sesion } from "../utils/session";
import { encabezado } from "../components/encabezado";

const app = document.querySelector<HTMLDivElement>("#app")!;

function render(): void {
    app.innerHTML = `
    ${encabezado()}
    <main class="login">
      <a class="volver" href="/">&larr; Volver al inicio</a>
      <h1>Ingresar</h1>

      <form id="form-login" novalidate>
        <label for="email">Email</label>
        <input type="email" id="email" name="email" required autocomplete="username" />

        <label for="password">Contraseña</label>
        <input type="password" id="password" name="password" required autocomplete="current-password" />

        <button type="submit">Ingresar</button>
      </form>
      
      <p>¿No tenés cuenta? <a id="link-registro" href="/registro.html">Registrate</a></p>

      <div id="mensaje" aria-live="polite"></div>
    </main>
  `;
}

// Recibe el HTML que arma el componente (mensajeError).
function mostrarMensaje(html: string): void {
    document.querySelector<HTMLElement>("#mensaje")!.innerHTML = html;
}

function irAlDestino(sesion: Sesion): void {
    const next = new URLSearchParams(window.location.search).get("next");
    window.location.href = "/" + destinoTrasLogin(sesion.rol, next);
}

async function manejarEnvio(event: SubmitEvent): Promise<void> {
    event.preventDefault();

    const form = event.currentTarget as HTMLFormElement;
    const boton = form.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    const email = form.querySelector<HTMLInputElement>("#email")!.value.trim();
    const password = form.querySelector<HTMLInputElement>("#password")!.value;

    if (!email || !password) {
        mostrarMensaje(mensajeError("Completá email y contraseña."));
        return;
    }

    mostrarMensaje("");

    // Se usa fetch y no apiPost: un 401 acá significa "credenciales incorrectas", y apiClient
    // lo trata como sesión vencida (borra la sesión y redirige al login).
    await ejecutarConBoton(
        boton,
        "Ingresando...",
        async () => {
            const response = await fetch(`${API_BASE_URL}/auth/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password }),
            });

            if (!response.ok) {
                let detalle = "No se pudo iniciar sesión.";
                try {
                    const error = await response.json();
                    detalle = error.message ?? detalle;
                } catch {
                    // Respuesta sin cuerpo JSON: se usa el mensaje generico.
                }
                throw new Error(detalle);
            }

            const sesion: Sesion = await response.json();
            guardarSesion(sesion);
            irAlDestino(sesion);
        },
        undefined, // se muestra el mensaje del backend
        (mensaje) => mostrarMensaje(mensajeError(mensaje)),
    );
}

function init(): void {
    const sesionActual = obtenerSesion();
    if (sesionActual) {
        irAlDestino(sesionActual);
        return;
    }
    render();

    const next = new URLSearchParams(window.location.search).get("next");
    if (next) {
        document.querySelector<HTMLAnchorElement>("#link-registro")!.href =
            `/registro.html?next=${encodeURIComponent(next)}`;
    }

    document.querySelector<HTMLFormElement>("#form-login")!.addEventListener("submit", manejarEnvio);
}

init();