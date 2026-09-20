import { apiGet } from "../data/apiClient";
import type { Edicion, CuraduriaEdicion } from "../types";

/**
 * Renderiza la pagina de "Ediciones" dentro del elemento contenedor dado.
 * Muestra la lista de ediciones, y por cada una un boton para cargar
 * sus 4 curadurias (categoria, libro, precio, cupo) bajo demanda.
 */
export async function renderEdicionesPage(container: HTMLElement) {
    container.innerHTML = "<h1>Ediciones</h1><p>Cargando ediciones...</p>";

    try {
        const ediciones = await apiGet<Edicion[]>("/ediciones");
        renderListaDeEdiciones(container, ediciones);
    } catch (error) {
        container.innerHTML = "<h1>Ediciones</h1><p> No se pudieron cargar las ediciones.</p>";
        console.error(error);
    }
}

function renderListaDeEdiciones(container: HTMLElement, ediciones: Edicion[]) {
    container.innerHTML = `
    <h1>Ediciones</h1>
    <ul id="lista-ediciones"></ul>
  `;

    const lista = container.querySelector("#lista-ediciones")!;

    ediciones.forEach((edicion) => {
        const item = document.createElement("li");
        item.innerHTML = `
      <strong>${edicion.nombre}</strong> — ${edicion.estado}
      (corte: ${edicion.fechaCorte})
      <button data-edicion-id="${edicion.id}">Ver detalle</button>
      <div class="curadurias" id="curadurias-${edicion.id}"></div>
    `;
        lista.appendChild(item);
    });

    // Un solo listener para todos los botones (delegacion de eventos),
    // en vez de agregar uno por cada boton individualmente.
    lista.addEventListener("click", async (event) => {
        const boton = event.target as HTMLElement;
        if (boton.tagName !== "BUTTON") return;

        const edicionId = boton.getAttribute("data-edicion-id");
        const contenedorCuradurias = container.querySelector(`#curadurias-${edicionId}`)!;

        if (contenedorCuradurias.innerHTML.trim() !== "") return;

        contenedorCuradurias.innerHTML = "Cargando...";

        try {
            const curadurias = await apiGet<CuraduriaEdicion[]>(
                `/ediciones/${edicionId}/curadurias`
            );
            renderCuradurias(contenedorCuradurias, curadurias);
        } catch (error) {
            contenedorCuradurias.innerHTML = " No se pudo cargar el detalle.";
            console.error(error);
        }
    });
}

function renderCuradurias(contenedor: Element, curadurias: CuraduriaEdicion[]) {
    contenedor.innerHTML = `
    <ul>
      ${curadurias
        .map(
            (c) => `
        <li>
          ${c.categoria.nombre}: "${c.libro.titulo}"
          — $${c.precioVigente} — cupo: ${c.cupoMaximo}
        </li>
      `
        )
        .join("")}
    </ul>
  `;
}