import { apiGet } from "../data/apiClient";
import type { Edicion, CuraduriaEdicion } from "../types";
import { tieneRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { formatearFecha, formatearMonto } from "../utils/formato";
import { mensajeCargando, mensajeError, mensajeVacio } from "../components/mensajes";

const ESTADO: Record<Edicion["estado"], string> = {
  ABIERTA: "Abierta",
  CERRADA: "Cerrada",
};

/**
* Pagina de "Ediciones". Al publico y a los suscriptores el backend les devuelve solo la edicion
* abierta y ven sus curadurias directamente; la administradora ve todas y las abre con "Ver detalle".
*/
export async function renderEdicionesPage(container: HTMLElement) {
  container.innerHTML = "<h1>Ediciones</h1>" + mensajeCargando("Cargando ediciones...");

  try {
    // El backend ya filtra: a quien no es ADMIN le devuelve solo la edición abierta.
    const ediciones = await apiGet<Edicion[]>("/ediciones");
    renderListaDeEdiciones(container, ediciones);
  } catch (error) {
    container.innerHTML = "<h1>Ediciones</h1>" + mensajeError("No se pudieron cargar las ediciones.");
    console.error(error);
  }
}

function renderListaDeEdiciones(container: HTMLElement, ediciones: Edicion[]) {
  const esAdmin = tieneRol("ADMIN");
  const titulo = esAdmin ? "Ediciones" : "Edición actual";

  if (ediciones.length === 0) {
    container.innerHTML = `<h1>${titulo}</h1>` + mensajeVacio("No hay una edición abierta por el momento.");
    return;
  }

  container.innerHTML = `
    <h1>${titulo}</h1>
    <ul id="lista-ediciones">
      ${ediciones.map((edicion) => renderEdicion(edicion, esAdmin)).join("")}
    </ul>
  `;

  // Publico y suscriptores: la edicion abierta muestra sus curadurias directamente.
  if (!esAdmin) {
    ediciones.forEach((edicion) => cargarDetalle(container, edicion));
    return;
  }

  // Administradora: pueden ser muchas ediciones, asi que el detalle se pide bajo demanda.
  const lista = container.querySelector<HTMLUListElement>("#lista-ediciones")!;

  // Un solo listener para todos los botones (delegacion de eventos),
  // en vez de agregar uno por cada boton individualmente.
  lista.addEventListener("click", async (event) => {
    const boton = event.target;
    if (!(boton instanceof HTMLButtonElement)) return;

    const edicionId = boton.dataset.edicionId;
    const detalle = container.querySelector<HTMLDivElement>(`#curadurias-${edicionId}`)!;

    // Si ya se cargo, el boton solo muestra u oculta el detalle (sin volver a pedirlo).
    if (detalle.dataset.cargado === "si") {
      detalle.hidden = !detalle.hidden;
      boton.textContent = detalle.hidden ? "Ver detalle" : "Ocultar detalle";
      return;
    }

    const edicion = ediciones.find((e) => String(e.id) === edicionId);
    if (!edicion) return;
    if (await cargarDetalle(container, edicion)) boton.textContent = "Ocultar detalle";
  });
}


/** Pide las curadurias de una edicion y las muestra en su tarjeta. Devuelve true si salio bien. */
async function cargarDetalle(container: HTMLElement, edicion: Edicion): Promise<boolean> {
  const detalle = container.querySelector<HTMLDivElement>(`#curadurias-${edicion.id}`)!;
  detalle.innerHTML = mensajeCargando("Cargando detalle...");

  try {
    const curadurias = await apiGet<CuraduriaEdicion[]>(`/ediciones/${edicion.id}/curadurias`);
    const permiteSuscripcion = edicion.estado === "ABIERTA" && !tieneRol("ADMIN");
    detalle.innerHTML = renderCuradurias(curadurias, permiteSuscripcion);
    detalle.dataset.cargado = "si";
    return true;
  } catch (error) {
    detalle.innerHTML = mensajeError("No se pudo cargar el detalle.");
    console.error(error);
    return false;
  }
}

function renderEdicion(edicion: Edicion, conBotonDetalle: boolean): string {
  return `
    <li class="edicion">
      <div class="edicion-cabecera">
        <h2>${escapar(edicion.nombre)}</h2>
        <span class="etiqueta etiqueta-${edicion.estado.toLowerCase()}">${ESTADO[edicion.estado]}</span>
      </div>
      <p class="edicion-fechas">
        Corte: ${formatearFecha(edicion.fechaCorte)} ·
        Despacho: del ${formatearFecha(edicion.fechaDespachoDesde)} al ${formatearFecha(edicion.fechaDespachoHasta)}
      </p>
      ${conBotonDetalle ? `<button data-edicion-id="${edicion.id}">Ver detalle</button>` : ""}
      <div class="curadurias" id="curadurias-${edicion.id}"></div>
    </li>
  `;
}

function renderCuradurias(curadurias: CuraduriaEdicion[], permiteSuscripcion: boolean): string {
  if (curadurias.length === 0) {
    return mensajeVacio("Esta edición todavía no tiene curadurías.");
  }

  return `
    <ul class="curadurias-grid">
      ${curadurias.map((c) => `
        <li class="curaduria">
          <h3>${escapar(c.categoria.nombre)}</h3>
          <p class="curaduria-libro">"${escapar(c.libro.titulo)}"</p>
          <p class="curaduria-datos">
            <span class="precio">${formatearMonto(c.precioVigente)}</span>
            <span>cupo: ${c.cupoMaximo ?? "sin límite"}</span>
          </p>
          ${permiteSuscripcion
      ? `<a class="boton" href="/suscripcion.html?categoriaId=${c.categoria.id}">Suscribirme</a>`
      : ""}
        </li>
      `).join("")}
    </ul>
  `;
}
