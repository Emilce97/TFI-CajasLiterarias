import { apiGet, apiPatch } from "../data/apiClient";
import type { Edicion, EstadoPedido, PedidoDespacho } from "../types";
import { exigirRol } from "../utils/auth";

/**
 * Panel de despacho: la administradora elige una edicion cerrada, ve sus pedidos
 * y los avanza de a un paso (empaquetar, despachar con tracking, entregar).
 */

// El circuito, igual que EstadoPedido.siguiente() del backend.
const SIGUIENTE: Record<EstadoPedido, EstadoPedido | null> = {
    PENDIENTE_DE_EMPAQUE: "EMPAQUETADO",
    EMPAQUETADO: "DESPACHADO",
    DESPACHADO: "ENTREGADO",
    ENTREGADO: null,
};

const ETIQUETA: Record<EstadoPedido, string> = {
    PENDIENTE_DE_EMPAQUE: "Pendiente de empaque",
    EMPAQUETADO: "Empaquetado",
    DESPACHADO: "Despachado",
    ENTREGADO: "Entregado",
};

export async function renderDespachoPage(container: HTMLElement) {
    if (!exigirRol("ADMIN")) return;
    container.innerHTML = "<h1>Panel de despacho</h1><p>Cargando...</p>";

    try {
        const ediciones = await apiGet<Edicion[]>("/ediciones");
        // Los pedidos se generan en el corte: solo hay pedidos en ediciones cerradas.
        const cerradas = ediciones.filter((edicion) => edicion.estado === "CERRADA");
        renderFiltros(container, cerradas);
    } catch (error) {
        container.innerHTML = "<h1>Panel de despacho</h1><p>No se pudieron cargar las ediciones.</p>";
        console.error(error);
    }
}

function renderFiltros(container: HTMLElement, cerradas: Edicion[]) {
    if (cerradas.length === 0) {
        container.innerHTML = `
      <h1>Panel de despacho</h1>
      <p>Todavía no hay ediciones cerradas. Los pedidos se generan al hacer el corte.</p>
    `;
        return;
    }

    const estados = Object.keys(ETIQUETA) as EstadoPedido[];

    container.innerHTML = `
    <h1>Panel de despacho</h1>
    <label>
      Edición:
      <select id="select-edicion">
        <option value="">Elegí una edición cerrada</option>
        ${cerradas.map((edicion) => `<option value="${edicion.id}">${escapar(edicion.nombre)}</option>`).join("")}
      </select>
    </label>
    <label>
      Estado:
      <select id="select-estado">
        <option value="">Todos</option>
        ${estados.map((estado) => `<option value="${estado}">${ETIQUETA[estado]}</option>`).join("")}
      </select>
    </label>
    <div id="tabla-pedidos"></div>
  `;

    const selectEdicion = container.querySelector<HTMLSelectElement>("#select-edicion")!;
    const selectEstado = container.querySelector<HTMLSelectElement>("#select-estado")!;
    const zonaTabla = container.querySelector<HTMLDivElement>("#tabla-pedidos")!;

    const recargar = async () => {
        if (selectEdicion.value) {
            await cargarPedidos(zonaTabla, Number(selectEdicion.value), selectEstado.value);
        } else {
            zonaTabla.innerHTML = "";
        }
    };

    selectEdicion.addEventListener("change", recargar);
    selectEstado.addEventListener("change", recargar);

    // Un solo listener para todos los botones (delegacion de eventos), como en Demanda.
    zonaTabla.addEventListener("click", async (event) => {
        const boton = event.target as HTMLElement;
        if (boton.tagName !== "BUTTON") return;

        const pedidoId = boton.getAttribute("data-pedido-id");
        const destino = boton.getAttribute("data-destino") as EstadoPedido;

        // Al despachar, el backend exige el tracking: lo pedimos antes de mandar.
        let tracking: string | undefined;
        if (destino === "DESPACHADO") {
            const ingresado = prompt("Número de tracking del envío:");
            if (ingresado === null) return; // la administradora cancelo
            if (ingresado.trim() === "") {
                alert("Para despachar hace falta el número de tracking.");
                return;
            }
            tracking = ingresado.trim();
        }

        boton.setAttribute("disabled", "true");
        boton.textContent = "Guardando...";

        try {
            await apiPatch(`/pedidos/${pedidoId}/estado`, { estado: destino, tracking });
            // Recargamos para ver el estado nuevo, quien lo cambio y cuando.
            await recargar();
        } catch (error) {
            boton.removeAttribute("disabled");
            boton.textContent = `Pasar a ${ETIQUETA[destino]}`;
            alert("No se pudo cambiar el estado. Recargá la lista y probá de nuevo.");
            console.error(error);
        }
    });
}

async function cargarPedidos(zona: HTMLElement, edicionId: number, estado: string) {
    zona.innerHTML = "<p>Cargando pedidos...</p>";
    const filtro = estado ? `?estado=${estado}` : "";

    try {
        const pedidos = await apiGet<PedidoDespacho[]>(`/ediciones/${edicionId}/pedidos${filtro}`);
        renderTabla(zona, pedidos);
    } catch (error) {
        zona.innerHTML = "<p>No se pudieron cargar los pedidos de esta edición.</p>";
        console.error(error);
    }
}

function renderTabla(zona: HTMLElement, pedidos: PedidoDespacho[]) {
    if (pedidos.length === 0) {
        zona.innerHTML = "<p>No hay pedidos para mostrar con este filtro.</p>";
        return;
    }

    zona.innerHTML = `
    <table>
      <thead>
        <tr>
          <th>N°</th>
          <th>Destinatario</th>
          <th>Dirección</th>
          <th>Caja</th>
          <th>Estado</th>
          <th>Tracking</th>
          <th>Observaciones</th>
          <th>Último cambio</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        ${pedidos.map(renderFila).join("")}
      </tbody>
    </table>
  `;
}

function renderFila(pedido: PedidoDespacho): string {
    const siguiente = SIGUIENTE[pedido.estado];
    const accion = siguiente
        ? `<button data-pedido-id="${pedido.id}" data-destino="${siguiente}">Pasar a ${ETIQUETA[siguiente]}</button>`
        : "—";
    const ultimoCambio = pedido.fechaModificacion
        ? `${formatearFecha(pedido.fechaModificacion)}<br>${escapar(pedido.modificadoPor)}`
        : "—";

    return `
    <tr>
      <td>${pedido.id}</td>
      <td>${escapar(pedido.nombreDestinatario)}</td>
      <td>${escapar(pedido.direccionEntrega)}</td>
      <td>${escapar(pedido.categoria)}<br>${escapar(pedido.libro)}</td>
      <td>${ETIQUETA[pedido.estado]}</td>
      <td>${escapar(pedido.tracking) || "—"}</td>
      <td>${escapar(pedido.observaciones)}</td>
      <td>${ultimoCambio}</td>
      <td>${accion}</td>
    </tr>
  `;
}

// "2026-10-17T17:19:57" → "17/10/2026 17:19". El backend ya manda la hora de Argentina:
// se arma a mano para no depender del formato regional de cada navegador.
function formatearFecha(iso: string): string {
    const [fecha, hora] = iso.split("T");
    const [anio, mes, dia] = fecha.split("-");
    return `${dia}/${mes}/${anio} ${hora.slice(0, 5)}`;
}

// El nombre y la direccion los cargan los suscriptores en el formulario publico:
// se escapan para que un texto con etiquetas HTML no se ejecute en el panel de la admin.
function escapar(texto: string | null): string {
    return (texto ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}