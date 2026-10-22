import { apiGet, apiPatch } from "../data/apiClient";
import type { Pago } from "../types";
import { exigirRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { formatearFecha, formatearMonto } from "../utils/formato";
import { mensajeCargando, mensajeError, mensajeVacio } from "../components/mensajes";
import { ejecutarConBoton } from "../components/botonAccion";

/**
 * Panel de conciliacion de pagos: lista los pagos pendientes de validar,
 * con un boton para validar cada uno.
 */
export async function renderPagosPage(container: HTMLElement) {
    if (!exigirRol("ADMIN")) return;
    container.innerHTML = `<h1>Pagos pendientes</h1>${mensajeCargando()}`;

    try {
        const pagos = await apiGet<Pago[]>("/pagos/pendientes");
        renderTablaDePagos(container, pagos);
    } catch (error) {
        container.innerHTML = `<h1>Pagos pendientes</h1>${mensajeError("No se pudieron cargar los pagos.")}`;
        console.error(error);
    }
}

function renderTablaDePagos(container: HTMLElement, pagos: Pago[]) {
    if (pagos.length === 0) {
        container.innerHTML = `<h1>Pagos pendientes</h1>${mensajeVacio("No hay pagos pendientes de validar.")}`;
        return;
    }

    container.innerHTML = `
    <h1>Pagos pendientes</h1>
    <table>
      <thead>
        <tr>
          <th>Suscriptor</th>
          <th>Temática</th>
          <th>Edición</th>
          <th>Monto</th>
          <th>Fecha de pago</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        ${pagos.map(renderFila).join("")}
      </tbody>
    </table>
  `;

    const tabla = container.querySelector("table")!;

    // Un solo listener para todos los botones (delegacion de eventos)
    tabla.addEventListener("click", async (event) => {
        const boton = event.target;
        if (!(boton instanceof HTMLButtonElement)) return;

        const ok = await ejecutarConBoton(
            boton,
            "Validando...",
            () => apiPatch(`/pagos/${boton.dataset.pagoId}/validar`),
            "No se pudo validar el pago.",
        );
        // Recargamos para que el pago validado ya no aparezca entre los pendientes.
        if (ok) await renderPagosPage(container);
    });
}

function renderFila(pago: Pago): string {
    const suscriptor = pago.suscripcion.suscriptor;
    return `
    <tr>
      <td>${escapar(suscriptor.nombre)}<br>${escapar(suscriptor.email)}</td>
      <td>${escapar(pago.suscripcion.categoria.nombre)}</td>
      <td>${escapar(pago.edicion.nombre)}</td>
      <td>${formatearMonto(pago.monto)}</td>
      <td>${formatearFecha(pago.fechaPago)}</td>
      <td><button data-pago-id="${pago.id}">Validar</button></td>
    </tr>
  `;
}