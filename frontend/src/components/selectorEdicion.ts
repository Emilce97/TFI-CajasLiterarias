import { apiGet } from "../data/apiClient";
import type { Edicion } from "../types";
import { escapar } from "../utils/html";

/** Trae solo las ediciones cerradas: las unicas que tienen pedidos y demanda (se generan en el corte). */
export async function cargarEdicionesCerradas(): Promise<Edicion[]> {
    const ediciones = await apiGet<Edicion[]>("/ediciones");
    return ediciones.filter((edicion) => edicion.estado === "CERRADA");
}

/**
 * Arma el <select> de ediciones, con una opcion vacia al principio.
 * El id sirve para encontrarlo despues con querySelector.
 */
export function selectorEdicion(
    ediciones: Edicion[],
    id = "select-edicion",
    textoVacio = "Elegí una edición cerrada",
): string {
    const opciones = ediciones
        .map((edicion) => `<option value="${edicion.id}">${escapar(edicion.nombre)}</option>`)
        .join("");

    return `
    <label>
      Edición:
      <select id="${id}">
        <option value="">${escapar(textoVacio)}</option>
        ${opciones}
      </select>
    </label>
  `;
}