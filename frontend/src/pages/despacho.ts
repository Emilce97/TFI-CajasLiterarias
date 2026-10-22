import { apiGet, apiPatch } from "../data/apiClient";
import type { Edicion, EstadoPedido, PedidoDespacho } from "../types";
import { exigirRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { formatearFechaHora } from "../utils/formato";
import { mensajeCargando, mensajeError, mensajeVacio } from "../components/mensajes";
import { cargarEdicionesCerradas, selectorEdicion } from "../components/selectorEdicion";
import { ejecutarConBoton } from "../components/botonAccion";
import { abrirRemito } from "./remito";

/**
 * Panel de despacho: la administradora elige una edicion cerrada, ve sus pedidos
 * y los avanza de a un paso (empaquetar, despachar con tracking, entregar).
 */

const TITULO = "<h1>Panel de despacho</h1>";

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
    container.innerHTML = TITULO + mensajeCargando();

    try {
        // Los pedidos se generan en el corte: solo hay pedidos en ediciones cerradas.
        const cerradas = await cargarEdicionesCerradas();
        renderFiltros(container, cerradas);
    } catch (error) {
        container.innerHTML = TITULO + mensajeError("No se pudieron cargar las ediciones.");
        console.error(error);
    }
}

function renderFiltros(container: HTMLElement, cerradas: Edicion[]) {
    if (cerradas.length === 0) {
        container.innerHTML = TITULO + mensajeVacio(
            "Todavía no hay ediciones cerradas. Los pedidos se generan al hacer el corte.");
        return;
    }

    const estados = Object.keys(ETIQUETA) as EstadoPedido[];

    container.innerHTML = `
    ${TITULO}
    ${selectorEdicion(cerradas)}
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
        const boton = event.target;
        if (!(boton instanceof HTMLButtonElement)) return;

        const remitoId = boton.dataset.remitoId;
        if (remitoId) {
            await abrirRemito(Number(remitoId));
            return;
        }

        const pedidoId = boton.dataset.pedidoId;
        const destino = boton.dataset.destino as EstadoPedido;

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

        const ok = await ejecutarConBoton(
            boton,
            "Guardando...",
            () => apiPatch(`/pedidos/${pedidoId}/estado`, { estado: destino, tracking }),
            "No se pudo cambiar el estado. Recargá la lista y probá de nuevo.",
        );
        // Recargamos para ver el estado nuevo, quien lo cambio y cuando.
        if (ok) await recargar();
    });
}

async function cargarPedidos(zona: HTMLElement, edicionId: number, estado: string) {
    zona.innerHTML = mensajeCargando("Cargando pedidos...");
    const filtro = estado ? `?estado=${estado}` : "";

    try {
        const pedidos = await apiGet<PedidoDespacho[]>(`/ediciones/${edicionId}/pedidos${filtro}`);
        renderTabla(zona, pedidos);
    } catch (error) {
        zona.innerHTML = mensajeError("No se pudieron cargar los pedidos de esta edición.");
        console.error(error);
    }
}

function renderTabla(zona: HTMLElement, pedidos: PedidoDespacho[]) {
    if (pedidos.length === 0) {
        zona.innerHTML = mensajeVacio("No hay pedidos para mostrar con este filtro.");
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
    const botonEstado = siguiente
        ? `<button data-pedido-id="${pedido.id}" data-destino="${siguiente}">Pasar a ${ETIQUETA[siguiente]}</button>`
        : "";
    // El remito se emite desde EMPAQUETADO en adelante (el backend lo valida igual).
    const botonRemito = pedido.estado !== "PENDIENTE_DE_EMPAQUE"
        ? `<button data-remito-id="${pedido.id}">Ver remito</button>`
        : "";

    const accion = [botonEstado, botonRemito].filter(Boolean).join(" ") || "—";
    const ultimoCambio = pedido.fechaModificacion
        ? `${formatearFechaHora(pedido.fechaModificacion)}<br>${escapar(pedido.modificadoPor)}`
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