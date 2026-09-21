import { API_BASE_URL } from '../data/apiClient'
import type { Categoria } from '../types';

interface CrearSuscripcionRequest {
  nombre: string;
  email: string;
  direccion: string;
  categoriaId: number;
}

const app = document.querySelector<HTMLDivElement>('#app')!;

function render(): void {
  app.innerHTML = `
    <main class="alta-suscripcion">
      <a class="volver" href="/">&larr; Volver a Ediciones</a>
      <h1>Sumate a tu caja literaria</h1>
      <p class="subtitulo">Completá tus datos para suscribirte a la temática elegida.</p>

      <form id="form-suscripcion" novalidate>
        <label for="nombre">Nombre y apellido</label>
        <input type="text" id="nombre" name="nombre" required autocomplete="name" />

        <label for="email">Email</label>
        <input type="email" id="email" name="email" required autocomplete="email" />

        <label for="direccion">Dirección de entrega</label>
        <input type="text" id="direccion" name="direccion" required autocomplete="street-address" />

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
    const response = await fetch(`${API_BASE_URL}/categorias`);
    if (!response.ok) {
      throw new Error('No se pudieron cargar las temáticas.');
    }
    const categorias: Categoria[] = await response.json();

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

  const nombre = form.querySelector<HTMLInputElement>('#nombre')!.value.trim();
  const email = form.querySelector<HTMLInputElement>('#email')!.value.trim();
  const direccion = form.querySelector<HTMLInputElement>('#direccion')!.value.trim();
  const categoriaId = form.querySelector<HTMLSelectElement>('#categoria')!.value;

  if (!nombre || !email || !direccion || !categoriaId) {
    mostrarMensaje('Completá todos los campos para continuar.', 'error');
    return;
  }

  const request: CrearSuscripcionRequest = {
    nombre,
    email,
    direccion,
    categoriaId: Number(categoriaId),
  };

  boton.disabled = true;
  mostrarMensaje('Enviando...', 'exito');

  try {
    const response = await fetch(`${API_BASE_URL}/suscripciones`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      let detalle = 'No se pudo completar la suscripción.';
      try {
        const error = await response.json();
        detalle = error.message ?? detalle;
      } catch {
        // Respuesta sin cuerpo JSON: se usa el mensaje genérico.
      }
      throw new Error(detalle);
    }

    mostrarMensaje('¡Listo! Tu suscripción quedó registrada.', 'exito');
    form.reset();
    // reset() deja seleccionada la primera temática; volvemos al placeholder.
    form.querySelector<HTMLSelectElement>('#categoria')!.selectedIndex = 0;
  } catch (error) {
    const detalle = error instanceof Error ? error.message : 'Ocurrió un error inesperado.';
    mostrarMensaje(detalle, 'error');
  } finally {
    boton.disabled = false;
  }
}

function init(): void {
  render();

  const params = new URLSearchParams(window.location.search);
  const categoriaIdPreseleccionada = params.get('categoriaId');

  void cargarCategorias(categoriaIdPreseleccionada);

  document
    .querySelector<HTMLFormElement>('#form-suscripcion')!
    .addEventListener('submit', manejarEnvio);
}

init();