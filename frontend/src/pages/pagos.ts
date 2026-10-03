import { apiGet, apiPatch } from "../data/apiClient";
import type { Pago } from "../types";

/**
 * Renderiza el panel de conciliacion de pagos: lista los pagos pendientes
 * de validar, con un boton para validar cada uno.
 */
export async function renderPagosPage(container: HTMLElement) {
    container.innerHTML = "<h1>Pagos pendientes</h1><p>Cargando...</p>";

    try {
        const pagos = await apiGet<Pago[]>("/pagos/pendientes");
        renderListaDePagos(container, pagos);
    } catch (error) {
        container.innerHTML = "<h1>Pagos pendientes</h1><p>❌ No se pudieron cargar los pagos.</p>";
        console.error(error);
    }
}

function renderListaDePagos(container: HTMLElement, pagos: Pago[]) {
    container.innerHTML = `
    <h1>Pagos pendientes</h1>
    ${pagos.length === 0 ? "<p>No hay pagos pendientes.</p>" : ""}
    <ul id="lista-pagos"></ul>
  `;

    const lista = container.querySelector("#lista-pagos")!;

    pagos.forEach((pago) => {
        const item = document.createElement("li");
        item.innerHTML = `
      <strong>${pago.suscripcion.suscriptor.nombre}</strong>
      — ${pago.suscripcion.categoria.nombre}
      — ${pago.edicion.nombre}
      — $${pago.monto}
      <button data-pago-id="${pago.id}">Validar</button>
    `;
        lista.appendChild(item);
    });

    // Un solo listener para todos los botones (delegacion de eventos)
    lista.addEventListener("click", async (event) => {
        const boton = event.target as HTMLElement;
        if (boton.tagName !== "BUTTON") return;

        const pagoId = boton.getAttribute("data-pago-id");
        boton.setAttribute("disabled", "true");
        boton.textContent = "Validando...";

        try {
            await apiPatch(`/pagos/${pagoId}/validar`);
            // Volvemos a pedir la lista completa, para que el pago recien
            // validado ya no aparezca entre los pendientes.
            await renderPagosPage(container);
        } catch (error) {
            boton.removeAttribute("disabled");
            boton.textContent = "Validar";
            alert("No se pudo validar el pago.");
            console.error(error);
        }
    });
}