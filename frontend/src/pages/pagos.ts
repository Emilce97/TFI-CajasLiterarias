import { apiGet, apiPatch } from "../data/apiClient";
import type { Pago } from "../types";
import { exigirRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { formatearFecha, formatearFechaHora, formatearMonto } from "../utils/formato";
import { mensajeCargando, mensajeError, mensajeVacio } from "../components/mensajes";
import { ejecutarConBoton } from "../components/botonAccion";

type Vista = "pendientes" | "validados";

/**
 * Panel de conciliacion de pagos. Dos vistas:
 * - pendientes: los pagos informados por los suscriptores, con un boton para validar cada uno;
 * - validados: el historial, con cuando y que administradora valido cada pago.
 */
export async function renderPagosPage(container: HTMLElement, vista: Vista = "pendientes") {
    if (!exigirRol("ADMIN")) return;

    container.innerHTML = `
    <h1>Pagos</h1>
    <label>
      Ver:
      <select id="vista-pagos">
        <option value="pendientes" ${vista === "pendientes" ? "selected" : ""}>Pendientes de validar</option>
        <option value="validados" ${vista === "validados" ? "selected" : ""}>Validados (historial)</option>
      </select>
    </label>
    <div id="zona-pagos">${mensajeCargando()}</div>
  `;

    const select = container.querySelector<HTMLSelectElement>("#vista-pagos")!;
    select.addEventListener("change", () => renderPagosPage(container, select.value as Vista));

    const zona = container.querySelector<HTMLDivElement>("#zona-pagos")!;
    try {
        const pagos = await apiGet<Pago[]>(`/pagos/${vista}`);
        if (vista === "pendientes") {
            renderPendientes(container, zona, pagos);
        } else {
            renderValidados(zona, pagos);
        }
    } catch (error) {
        zona.innerHTML = mensajeError("No se pudieron cargar los pagos.");
        console.error(error);
    }
}

function renderPendientes(container: HTMLElement, zona: HTMLElement, pagos: Pago[]) {
    if (pagos.length === 0) {
        zona.innerHTML = mensajeVacio("No hay pagos pendientes de validar.");
        return;
    }

    zona.innerHTML = `
    <table>
      <thead>
        <tr>${encabezadosComunes()}<th></th></tr>
      </thead>
      <tbody>
        ${pagos.map((pago) => `
          <tr>
            ${celdasComunes(pago)}
            <td><button data-pago-id="${pago.id}">Validar</button></td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;

    const tabla = zona.querySelector("table")!;

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
        if (ok) await renderPagosPage(container, "pendientes");
    });
}

function renderValidados(zona: HTMLElement, pagos: Pago[]) {
    if (pagos.length === 0) {
        zona.innerHTML = mensajeVacio("Todavía no hay pagos validados.");
        return;
    }

    zona.innerHTML = `
    <table>
      <thead>
        <tr>${encabezadosComunes()}<th>Validado</th></tr>
      </thead>
      <tbody>
        ${pagos.map((pago) => `
          <tr>
            ${celdasComunes(pago)}
            <td>${formatearFechaHora(pago.fechaValidacion)}<br>${escapar(pago.validadoPor?.email ?? null)}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

// Columnas que comparten las dos vistas.
function encabezadosComunes(): string {
    return `
    <th>Suscriptor</th>
    <th>Temática</th>
    <th>Edición</th>
    <th>Monto</th>
    <th>Fecha de pago</th>
  `;
}

function celdasComunes(pago: Pago): string {
    const suscriptor = pago.suscripcion.suscriptor;
    return `
    <td>${escapar(suscriptor.nombre)}<br>${escapar(suscriptor.email)}</td>
    <td>${escapar(pago.suscripcion.categoria.nombre)}</td>
    <td>${escapar(pago.edicion.nombre)}</td>
    <td>${formatearMonto(pago.monto)}</td>
    <td>${formatearFecha(pago.fechaPago)}</td>
  `;
}