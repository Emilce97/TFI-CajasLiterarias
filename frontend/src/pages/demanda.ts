import { apiGet, apiPatch } from "../data/apiClient";
import type { DemandaItem, Edicion } from "../types";
import { exigirRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { mensajeCargando, mensajeError, mensajeVacio } from "../components/mensajes";
import { cargarEdicionesCerradas, selectorEdicion } from "../components/selectorEdicion";
import { ejecutarConBoton } from "../components/botonAccion";

const TITULO = "<h1>Demanda para proveedores</h1>";

/**
 * Panel de demanda para proveedores: la administradora elige una edicion cerrada,
 * ve cuantos libros tiene que pedirle a cada proveedor y registra lo que va llegando.
 */
export async function renderDemandaPage(container: HTMLElement) {
    if (!exigirRol("ADMIN")) return;
    container.innerHTML = TITULO + mensajeCargando();

    try {
        // La demanda se calcula en el corte: solo tiene sentido para ediciones cerradas.
        const cerradas = await cargarEdicionesCerradas();
        renderSelectorDeEdicion(container, cerradas);
    } catch (error) {
        container.innerHTML = TITULO + mensajeError("No se pudieron cargar las ediciones.");
        console.error(error);
    }
}

function renderSelectorDeEdicion(container: HTMLElement, cerradas: Edicion[]) {
    if (cerradas.length === 0) {
        container.innerHTML = TITULO + mensajeVacio(
            "Todavía no hay ediciones cerradas. La demanda se calcula al hacer el corte.");
        return;
    }

    container.innerHTML = `${TITULO}${selectorEdicion(cerradas)}<div id="tabla-demanda"></div>`;

    const select = container.querySelector<HTMLSelectElement>("#select-edicion")!;
    const zonaTabla = container.querySelector<HTMLDivElement>("#tabla-demanda")!;

    select.addEventListener("change", () => {
        if (select.value) {
            cargarDemanda(zonaTabla, Number(select.value));
        } else {
            zonaTabla.innerHTML = "";
        }
    });

    // Un solo listener para todos los botones "Guardar" (delegacion de eventos).
    // Se registra una vez sobre la zona, que no se reemplaza: solo cambia su contenido.
    zonaTabla.addEventListener("click", async (event) => {
        const boton = event.target;
        if (!(boton instanceof HTMLButtonElement)) return;

        const demandaId = boton.dataset.demandaId;
        const input = zonaTabla.querySelector<HTMLInputElement>(`input[data-demanda-id="${demandaId}"]`)!;
        const cantidad = Number(input.value);

        if (input.value === "" || !Number.isInteger(cantidad) || cantidad < 0) {
            alert("Ingresá una cantidad entera, cero o mayor.");
            return;
        }

        const ok = await ejecutarConBoton(
            boton,
            "Guardando...",
            () => apiPatch(`/demanda/${demandaId}/recepcion`, { cantidadRecibida: cantidad }),
            "No se pudo registrar la recepción.",
        );
        // Recargamos la demanda para ver el faltante y el estado recalculados.
        if (ok) await cargarDemanda(zonaTabla, Number(select.value));
    });
}

async function cargarDemanda(zona: HTMLElement, edicionId: number) {
    zona.innerHTML = mensajeCargando("Cargando demanda...");

    try {
        const filas = await apiGet<DemandaItem[]>(`/ediciones/${edicionId}/demanda`);
        renderTablas(zona, filas);
    } catch (error) {
        zona.innerHTML = mensajeError("No se pudo cargar la demanda de esta edición.");
        console.error(error);
    }
}

/**
 * Arma una tabla por proveedor. El backend ya devuelve las filas ordenadas
 * por proveedor y titulo, asi que alcanza con agruparlas en ese orden.
 */
function renderTablas(zona: HTMLElement, filas: DemandaItem[]) {
    if (filas.length === 0) {
        zona.innerHTML = mensajeVacio("Esta edición no generó pedidos, así que no hay libros para pedir.");
        return;
    }

    const porProveedor = new Map<string, DemandaItem[]>();
    filas.forEach((fila) => {
        const grupo = porProveedor.get(fila.proveedorNombre) ?? [];
        grupo.push(fila);
        porProveedor.set(fila.proveedorNombre, grupo);
    });

    zona.innerHTML = [...porProveedor.entries()]
        .map(([proveedor, grupo]) => `
      <h2>${escapar(proveedor)}</h2>
      <table>
        <thead>
          <tr>
            <th>Libro</th>
            <th>Autor</th>
            <th>Requeridos</th>
            <th>Recibidos</th>
            <th>Faltan</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${grupo.map(renderFila).join("")}
        </tbody>
      </table>
    `)
        .join("");
}

function renderFila(fila: DemandaItem): string {
    const estado = fila.estadoFaltante === "FALTANTE" ? "Faltante" : "Completo";
    return `
    <tr>
      <td>${escapar(fila.titulo)}</td>
      <td>${escapar(fila.autor)}</td>
      <td>${fila.cantidadRequerida}</td>
      <td>
        <input type="number" min="0" step="1" value="${fila.cantidadRecibida}"
               data-demanda-id="${fila.id}" style="width: 5em" />
      </td>
      <td>${fila.cantidadFaltante}</td>
      <td>${estado}</td>
      <td><button data-demanda-id="${fila.id}">Guardar</button></td>
    </tr>
  `;
}