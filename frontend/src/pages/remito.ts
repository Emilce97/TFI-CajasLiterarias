import { apiGet } from "../data/apiClient";
import type { Remito } from "../types";
import { escapar } from "../utils/html";

/**
 * Remito de un pedido: se muestra en un dialogo sobre el panel de despacho
 * (asi no se pierden los filtros) y se puede imprimir.
 */
export async function abrirRemito(pedidoId: number) {
    let remito: Remito;
    try {
        remito = await apiGet<Remito>(`/pedidos/${pedidoId}/remito`);
    } catch (error) {
        alert("No se pudo generar el remito. Verificá que el pedido esté empaquetado.");
        console.error(error);
        return;
    }

    document.querySelector("#remito-dialog")?.remove();
    const dialogo = document.createElement("dialog");
    dialogo.id = "remito-dialog";
    dialogo.innerHTML = `
    <article>
      <h2>Remito N° ${String(remito.numero).padStart(6, "0")}</h2>
      <p>Fecha de emisión: ${formatearFecha(remito.fechaEmision)}</p>
      <p>Edición: ${escapar(remito.edicion)}</p>
      <h3>Destinatario</h3>
      <p>${escapar(remito.nombreDestinatario)}</p>
      <p>${escapar(remito.direccionEntrega)}</p>
      <h3>Contenido de la caja</h3>
      <p>Categoría: ${escapar(remito.categoria)}</p>
      <p>Libro: ${escapar(remito.libro)}</p>
      ${remito.tracking ? `<p>Tracking: ${escapar(remito.tracking)}</p>` : ""}
    </article>
    <div class="remito-acciones">
      <button id="remito-imprimir">Imprimir</button>
      <button id="remito-cerrar">Cerrar</button>
    </div>
  `;
    document.body.appendChild(dialogo);

    dialogo.querySelector("#remito-imprimir")!.addEventListener("click", () => window.print());
    dialogo.querySelector("#remito-cerrar")!.addEventListener("click", () => dialogo.close());
    dialogo.addEventListener("close", () => dialogo.remove());
    dialogo.showModal();
}

// "2026-10-17" → "17/10/2026" (armado a mano, igual que en el panel de despacho)
function formatearFecha(iso: string): string {
    const [anio, mes, dia] = iso.split("-");
    return `${dia}/${mes}/${anio}`;
}