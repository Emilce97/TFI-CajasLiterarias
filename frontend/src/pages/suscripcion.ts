import { apiGet, apiPost } from '../data/apiClient';
import type { Categoria } from '../types';
import { exigirRol, paginaActual } from '../utils/auth';
import { escapar } from '../utils/html';
import { obtenerSesion } from '../utils/session';

interface CrearSuscripcionRequest {
  categoriaId: number;
}

const app = document.querySelector<HTMLDivElement>('#app')!;

function render(nombre: string): void {
  app.innerHTML = `
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

      <p id="mensaje" class="mensaje" role="status" aria-live="polite"></p>
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
        .map((categoria) => `<option value="${categoria.id}">${categoria.nombre}</option>`)
        .join('');

    if (categoriaIdPreseleccionada) {
      select.value = categoriaIdPreseleccionada;
    } else {
      select.selectedIndex = 0;
    }
  } catch (error) {
    select.innerHTML = '<option value="" disabled selected>No se pudieron cargar las temáticas</option>';
    mostrarMensaje('No pudimos cargar las temáticas disponibles. Recargá la página para reintentar.', 'error');
  }
}

function mostrarMensaje(texto: string, tipo: 'exito' | 'error'): void {
  const mensaje = document.querySelector<HTMLParagraphElement>('#mensaje')!;
  mensaje.textContent = texto;
  mensaje.className = `mensaje ${tipo}`;
}

async function manejarEnvio(event: SubmitEvent): Promise<void> {
  event.preventDefault();

  const form = event.currentTarget as HTMLFormElement;
  const boton = form.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const categoriaId = form.querySelector<HTMLSelectElement>('#categoria')!.value;

  if (!categoriaId) {
    mostrarMensaje('Elegí una temática para continuar.', 'error');
    return;
  }

  const request: CrearSuscripcionRequest = { categoriaId: Number(categoriaId) };

  boton.disabled = true;
  mostrarMensaje('Enviando...', 'exito');

  try {
    await apiPost('/suscripciones', request);
    mostrarMensaje('¡Listo! Tu suscripción quedó registrada.', 'exito');
    // Volvemos al placeholder de la temática.
    form.querySelector<HTMLSelectElement>('#categoria')!.selectedIndex = 0;
  } catch (error) {
    // El mensaje viene del backend (cupo completo, ya suscripta, ventana cerrada...).
    mostrarMensaje(error instanceof Error ? error.message : 'Ocurrió un error inesperado.', 'error');
  } finally {
    boton.disabled = false;
  }
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