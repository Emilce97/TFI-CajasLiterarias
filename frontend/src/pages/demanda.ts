import { apiGet, apiPatch } from "../data/apiClient";
import type { DemandaItem, Edicion } from "../types";

/**
 * Panel de demanda para proveedores: la administradora elige una edicion cerrada,
 * ve cuantos libros tiene que pedirle a cada proveedor y registra lo que va llegando.
 */
export async function renderDemandaPage(container: HTMLElement) {
    container.innerHTML = "<h1>Demanda para proveedores</h1><p>Cargando...</p>";

    try {
        const ediciones = await apiGet<Edicion[]>("/ediciones");
        // La demanda se calcula en el corte: solo tiene sentido para ediciones cerradas.
        const cerradas = ediciones.filter((edicion) => edicion.estado === "CERRADA");
        renderSelectorDeEdicion(container, cerradas);
    } catch (error) {
        container.innerHTML = "<h1>Demanda para proveedores</h1><p> No se pudieron cargar las ediciones.</p>";
        console.error(error);
    }
}

function renderSelectorDeEdicion(container: HTMLElement, cerradas: Edicion[]) {
    if (cerradas.length === 0) {
        container.innerHTML = `
      <h1>Demanda para proveedores</h1>
      <p>Todavía no hay ediciones cerradas. La demanda se calcula al hacer el corte.</p>
    `;
        return;
    }

    container.innerHTML = `
    <h1>Demanda para proveedores</h1>
    <label>
      Edición:
      <select id="select-edicion">
        <option value="">Elegí una edición cerrada</option>
        ${cerradas.map((edicion) => `<option value="${edicion.id}">${edicion.nombre}</option>`).join("")}
      </select>
    </label>
    <div id="tabla-demanda"></div>
  `;

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
        const boton = event.target as HTMLElement;
        if (boton.tagName !== "BUTTON") return;

        const demandaId = boton.getAttribute("data-demanda-id");
        const input = zonaTabla.querySelector<HTMLInputElement>(`input[data-demanda-id="${demandaId}"]`)!;
        const cantidad = Number(input.value);

        if (input.value === "" || !Number.isInteger(cantidad) || cantidad < 0) {
            alert("Ingresá una cantidad entera, cero o mayor.");
            return;
        }

        boton.setAttribute("disabled", "true");
        boton.textContent = "Guardando...";

        try {
            await apiPatch(`/demanda/${demandaId}/recepcion`, { cantidadRecibida: cantidad });
            // Recargamos la demanda para ver el faltante y el estado recalculados.
            await cargarDemanda(zonaTabla, Number(select.value));
        } catch (error) {
            boton.removeAttribute("disabled");
            boton.textContent = "Guardar";
            alert("No se pudo registrar la recepción.");
            console.error(error);
        }
    });
}

async function cargarDemanda(zona: HTMLElement, edicionId: number) {
    zona.innerHTML = "<p>Cargando demanda...</p>";

    try {
        const filas = await apiGet<DemandaItem[]>(`/ediciones/${edicionId}/demanda`);
        renderTablas(zona, filas);
    } catch (error) {
        zona.innerHTML = "<p> No se pudo cargar la demanda de esta edición.</p>";
        console.error(error);
    }
}

/**
 * Arma una tabla por proveedor. El backend ya devuelve las filas ordenadas
 * por proveedor y titulo, asi que alcanza con agruparlas en ese orden.
 */
function renderTablas(zona: HTMLElement, filas: DemandaItem[]) {
    if (filas.length === 0) {
        zona.innerHTML = "<p>Esta edición no generó pedidos, así que no hay libros para pedir.</p>";
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
      <h2>${proveedor}</h2>
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
    const estado = fila.estadoFaltante === "FALTANTE" ? " Faltante" : " Completo";
    return `
    <tr>
      <td>${fila.titulo}</td>
      <td>${fila.autor ?? ""}</td>
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