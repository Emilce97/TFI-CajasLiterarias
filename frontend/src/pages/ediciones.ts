import { apiGet, apiPost } from "../data/apiClient";
import type { Edicion, CuraduriaEdicion } from "../types";
import { tieneRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { formatearFecha, formatearMonto } from "../utils/formato";
import { mensajeCargando, mensajeError, mensajeExito, mensajeVacio } from "../components/mensajes";
import { renderFormularioNuevaEdicion } from "./nuevaEdicion";

const ESTADO: Record<Edicion["estado"], string> = {
  ABIERTA: "Abierta",
  CERRADA: "Cerrada",
};

/** Lo que devuelve POST /api/ediciones/{id}/cierre (ResumenCierreDTO del backend). */
interface ResumenCierre {
  edicionId: number;
  totalPedidosGenerados: number;
  totalExcluidos: number;
  suscripcionesExcluidas: number[];
}

/**
 * Pagina de "Ediciones". Al publico y a los suscriptores el backend les devuelve solo la edicion
 * abierta y ven sus curadurias directamente; la administradora ve todas y las abre con "Ver detalle".
 * La administradora ademas puede cerrar la edicion abierta (corte) y, si no hay ninguna abierta,
 * ve el formulario para crear una.
 * "aviso" es un mensaje de exito opcional (ej. despues de crear o cerrar una edicion).
 */
export async function renderEdicionesPage(container: HTMLElement, aviso?: string) {
  container.innerHTML = "<h1>Ediciones</h1>" + mensajeCargando("Cargando ediciones...");

  try {
    // El backend ya filtra: a quien no es ADMIN le devuelve solo la edición abierta.
    const ediciones = await apiGet<Edicion[]>("/ediciones");
    renderListaDeEdiciones(container, ediciones, aviso);
  } catch (error) {
    container.innerHTML = "<h1>Ediciones</h1>" + mensajeError("No se pudieron cargar las ediciones.");
    console.error(error);
  }
}

function renderListaDeEdiciones(container: HTMLElement, ediciones: Edicion[], aviso?: string) {
  const esAdmin = tieneRol("ADMIN");
  const titulo = esAdmin ? "Ediciones" : "Edición actual";
  // Solo puede haber una edicion abierta a la vez (regla del backend): el formulario aparece si no hay ninguna.
  const conFormulario = esAdmin && !ediciones.some((edicion) => edicion.estado === "ABIERTA");

  if (ediciones.length === 0 && !conFormulario) {
    container.innerHTML = `<h1>${titulo}</h1>` + mensajeVacio("No hay una edición abierta por el momento.");
    return;
  }

  container.innerHTML = `
    <h1>${titulo}</h1>
    ${aviso ? mensajeExito(aviso) : ""}
    ${conFormulario ? '<section id="nueva-edicion"></section>' : ""}
    ${ediciones.length > 0 ? `
      <ul id="lista-ediciones">
        ${ediciones.map((edicion) => renderEdicion(edicion, esAdmin)).join("")}
      </ul>` : ""}
  `;

  if (conFormulario) {
    const zona = container.querySelector<HTMLElement>("#nueva-edicion")!;
    renderFormularioNuevaEdicion(zona, (edicion) =>
        renderEdicionesPage(container,
            `Edición "${edicion.nombre}" creada. Ya está abierta para suscripciones y pagos.`));
  }

  if (ediciones.length === 0) return;

  // Publico y suscriptores: la edicion abierta muestra sus curadurias directamente.
  if (!esAdmin) {
    ediciones.forEach((edicion) => cargarDetalle(container, edicion));
    return;
  }

  // Administradora: pueden ser muchas ediciones, asi que el detalle se pide bajo demanda.
  const lista = container.querySelector<HTMLUListElement>("#lista-ediciones")!;

  // Un solo listener para todos los botones (delegacion de eventos),
  // en vez de agregar uno por cada boton individualmente.
  // Cada boton dice que hace con data-accion: "detalle" o "cerrar".
  lista.addEventListener("click", async (event) => {
    const boton = event.target;
    if (!(boton instanceof HTMLButtonElement)) return;

    const edicionId = boton.dataset.edicionId;
    const edicion = ediciones.find((e) => String(e.id) === edicionId);
    if (!edicion) return;

    if (boton.dataset.accion === "cerrar") {
      await cerrarEdicion(container, edicion, boton);
      return;
    }

    const detalle = container.querySelector<HTMLDivElement>(`#curadurias-${edicionId}`)!;

    // Si ya se cargo, el boton solo muestra u oculta el detalle (sin volver a pedirlo).
    if (detalle.dataset.cargado === "si") {
      detalle.hidden = !detalle.hidden;
      boton.textContent = detalle.hidden ? "Ver detalle" : "Ocultar detalle";
      return;
    }

    if (await cargarDetalle(container, edicion)) boton.textContent = "Ocultar detalle";
  });
}

/**
 * Corte de la edicion: genera los pedidos y la cierra. No se puede deshacer, por eso se pide confirmacion.
 * Si sale bien, recarga la pagina con el resumen (y aparece el formulario para crear la siguiente).
 */
async function cerrarEdicion(container: HTMLElement, edicion: Edicion, boton: HTMLButtonElement) {
  const confirmado = confirm(
      `¿Cerrar la edición "${edicion.nombre}"?\n\n` +
      "Se van a generar los pedidos de las suscripciones con pago validado y la edición " +
      "ya no se va a poder volver a abrir.");
  if (!confirmado) return;

  const resultado = container.querySelector<HTMLDivElement>(`#cierre-${edicion.id}`)!;
  resultado.innerHTML = "";
  boton.disabled = true;
  boton.textContent = "Cerrando...";

  try {
    const resumen = await apiPost<ResumenCierre>(`/ediciones/${edicion.id}/cierre`, {});
    renderEdicionesPage(container, textoResumen(edicion, resumen));
  } catch (error) {
    // El backend valida (ej. la edicion ya estaba cerrada): mostramos su mensaje.
    resultado.innerHTML = mensajeError(
        error instanceof Error ? error.message : "No se pudo cerrar la edición.");
    boton.disabled = false;
    boton.textContent = "Cerrar edición";
    console.error(error);
  }
}

function textoResumen(edicion: Edicion, resumen: ResumenCierre): string {
  const pedidos = resumen.totalPedidosGenerados === 1
      ? "Se generó 1 pedido"
      : `Se generaron ${resumen.totalPedidosGenerados} pedidos`;
  const excluidos = resumen.totalExcluidos === 0
      ? ""
      : resumen.totalExcluidos === 1
          ? " 1 suscripción quedó afuera del corte."
          : ` ${resumen.totalExcluidos} suscripciones quedaron afuera del corte.`;
  return `Edición "${edicion.nombre}" cerrada. ${pedidos}.${excluidos}`;
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

function renderEdicion(edicion: Edicion, esAdmin: boolean): string {
  const conCierre = esAdmin && edicion.estado === "ABIERTA";
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
      ${esAdmin ? `<button data-accion="detalle" data-edicion-id="${edicion.id}">Ver detalle</button>` : ""}
      ${conCierre ? `<button data-accion="cerrar" data-edicion-id="${edicion.id}">Cerrar edición</button>` : ""}
      <div id="cierre-${edicion.id}" aria-live="polite"></div>
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