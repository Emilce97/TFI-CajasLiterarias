import { apiGet, apiPost } from '../data/apiClient';
import type { Categoria } from '../types';
import { ejecutarConBoton } from '../components/botonAccion';
import { mensajeError, mensajeExito } from '../components/mensajes';
import { exigirRol, paginaActual } from '../utils/auth';
import { escapar } from '../utils/html';
import { obtenerSesion } from '../utils/session';
import { encabezado } from '../components/encabezado';

interface CrearSuscripcionRequest {
  categoriaId: number;
}

const app = document.querySelector<HTMLDivElement>('#app')!;

function render(nombre: string): void {
  app.innerHTML = `
    ${encabezado()}
    <main class="alta-suscripcion">
      <a class="volver" href="/">&larr; Volver a Ediciones</a>
      <h1>Sumate a tu caja literaria</h1>
      <p class="subtitulo">Hola, ${escapar(nombre)}. Elegí la temática a la que querés suscribirte.</p>

      <form id="form-suscripcion" novalidate>
        <label for="categoria">Temática</label>
        <select id="categoria" name="categoria" required>
          <option value="" disabled selected>Cargando temáticas...</option>
        </select>

        <button type="submit">Suscribirme</button>
      </form>

      <div id="mensaje" aria-live="polite"></div>
    </main>
  `;
}

async function cargarCategorias(categoriaIdPreseleccionada: string | null): Promise<void> {
  const select = document.querySelector<HTMLSelectElement>('#categoria')!;

  try {
    const categorias = await apiGet<Categoria[]>('/categorias');

    select.innerHTML =
      '<option value="" disabled>Elegí una temática</option>' +
      categorias
        .map((categoria) => `<option value="${categoria.id}">${escapar(categoria.nombre)}</option>`)
        .join('');

    if (categoriaIdPreseleccionada) {
      select.value = categoriaIdPreseleccionada;
    } else {
      select.selectedIndex = 0;
    }
  } catch (error) {
    select.innerHTML = '<option value="" disabled selected>No se pudieron cargar las temáticas</option>';
    mostrarMensaje(mensajeError('No pudimos cargar las temáticas disponibles. Recargá la página para reintentar.'));
  }
}

// Recibe el HTML que arman los componentes (mensajeError, mensajeExito).
function mostrarMensaje(html: string): void {
  document.querySelector<HTMLElement>('#mensaje')!.innerHTML = html;
}

async function manejarEnvio(event: SubmitEvent): Promise<void> {
  event.preventDefault();

  const form = event.currentTarget as HTMLFormElement;
  const boton = form.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const select = form.querySelector<HTMLSelectElement>('#categoria')!;

  if (!select.value) {
    mostrarMensaje(mensajeError('Elegí una temática para continuar.'));
    return;
  }

  const request: CrearSuscripcionRequest = { categoriaId: Number(select.value) };
  mostrarMensaje('');

  await ejecutarConBoton(
    boton,
    'Enviando...',
    async () => {
      await apiPost('/suscripciones', request);
      mostrarMensaje(mensajeExito('¡Listo! Tu suscripción quedó registrada.'));
      // Volvemos al placeholder de la temática.
      select.selectedIndex = 0;
    },
    // Sin texto fijo: se muestra el mensaje del backend (cupo completo, ya suscripta, ventana cerrada...).
    undefined,
    (mensaje) => mostrarMensaje(mensajeError(mensaje)),
  );
}

function init(): void {
  // Sin sesión de suscriptor: login y vuelta acá (con la temática elegida).
  if (!exigirRol('SUSCRIPTOR', paginaActual())) return;

  render(obtenerSesion()?.nombre ?? '');

  const params = new URLSearchParams(window.location.search);
  void cargarCategorias(params.get('categoriaId'));

  document
    .querySelector<HTMLFormElement>('#form-suscripcion')!
    .addEventListener('submit', manejarEnvio);
}

// Si el navegador restaura la pagina desde su cache (boton "atras" despues de cerrar sesion),
// se recarga para que no muestre una vista protegida vieja.
window.addEventListener('pageshow', (event) => {
  if (event.persisted) window.location.reload();
});

init();