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
 * Pagina de "Ediciones": la lista de ediciones y, por cada una, un boton para
 * ver sus curadurias (categoria, libro, precio, cupo), que se cargan bajo demanda.
 */
export async function renderEdicionesPage(container: HTMLElement) {
    container.innerHTML = "<h1>Ediciones</h1>" + mensajeCargando("Cargando ediciones...");

    try {
        const todas = await apiGet<Edicion[]>("/ediciones");
        // Visitantes: solo la edición abierta. Administradora: todas (para ver el historial).
        const visibles = tieneRol("ADMIN")
            ? todas
            : todas.filter((edicion) => edicion.estado === "ABIERTA");
        renderListaDeEdiciones(container, visibles);
    } catch (error) {
        container.innerHTML = "<h1>Ediciones</h1>" + mensajeError("No se pudieron cargar las ediciones.");
        console.error(error);
    }
}

function renderListaDeEdiciones(container: HTMLElement, ediciones: Edicion[]) {
    const titulo = tieneRol("ADMIN") ? "Ediciones" : "Edición actual";

    if (ediciones.length === 0) {
        container.innerHTML = `<h1>${titulo}</h1>` + mensajeVacio("No hay una edición abierta por el momento.");
        return;
    }

    container.innerHTML = `
    <h1>${titulo}</h1>
    <ul id="lista-ediciones">
      ${ediciones.map(renderEdicion).join("")}
    </ul>
  `;

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

        detalle.innerHTML = mensajeCargando("Cargando detalle...");

        try {
            const curadurias = await apiGet<CuraduriaEdicion[]>(`/ediciones/${edicionId}/curadurias`);
            const edicion = ediciones.find((e) => String(e.id) === edicionId);
            const permiteSuscripcion = edicion?.estado === "ABIERTA" && !tieneRol("ADMIN");
            detalle.innerHTML = renderCuradurias(curadurias, permiteSuscripcion);
            detalle.dataset.cargado = "si";
            boton.textContent = "Ocultar detalle";
        } catch (error) {
            detalle.innerHTML = mensajeError("No se pudo cargar el detalle.");
            console.error(error);
        }
    });
}

function renderEdicion(edicion: Edicion): string {
    return `
    <li class="edicion">
      <strong>${escapar(edicion.nombre)}</strong> 
      <span class="etiqueta etiqueta-${edicion.estado.toLowerCase()}">${ESTADO[edicion.estado]}</span>
      <br>
      Corte: ${formatearFecha(edicion.fechaCorte)} ·
      Despacho: del ${formatearFecha(edicion.fechaDespachoDesde)} al ${formatearFecha(edicion.fechaDespachoHasta)}
      <br>
      <button data-edicion-id="${edicion.id}">Ver detalle</button>
      <div class="curadurias" id="curadurias-${edicion.id}"></div>
    </li>
  `;
}

function renderCuradurias(curadurias: CuraduriaEdicion[], permiteSuscripcion: boolean): string {
    return `
    <ul>
      ${curadurias.map((c) => `
        <li>
          <strong>${escapar(c.categoria.nombre)}</strong>: "${escapar(c.libro.titulo)}"
          — ${formatearMonto(c.precioVigente)} — cupo: ${c.cupoMaximo ?? "sin límite"}
          ${permiteSuscripcion
        ? `— <a href="/suscripcion.html?categoriaId=${c.categoria.id}">Suscribirme</a>`
        : ""}
        </li>
      `).join("")}
    </ul>
  `;
}