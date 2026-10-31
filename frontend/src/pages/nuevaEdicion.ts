import { apiGet, apiPost } from "../data/apiClient";
import type { Categoria, Edicion, Libro } from "../types";
import { escapar } from "../utils/html";
import { mensajeCargando, mensajeError } from "../components/mensajes";

/**
 * Formulario "Nueva edicion" (solo ADMIN): datos generales, un precio unico para las 4 tematicas
 * y, por cada tematica, el libro del mes y el cupo maximo.
 * El backend vuelve a validar todo; aca se controla antes para dar mensajes claros.
 */

interface CuraduriaNueva {
    categoriaId: number;
    libroId: number | null;
    precioVigente: number;
    cupoMaximo: number | null;
}

export async function renderFormularioNuevaEdicion(
    zona: HTMLElement,
    alCrear: (edicion: Edicion) => void,
) {
    zona.innerHTML = mensajeCargando("Cargando formulario...");

    try {
        const [categorias, libros] = await Promise.all([
            apiGet<Categoria[]>("/categorias"),
            apiGet<Libro[]>("/libros"),
        ]);
        if (libros.length === 0) {
            zona.innerHTML = mensajeError("No hay libros cargados para armar una edición.");
            return;
        }
        dibujarFormulario(zona, categorias, libros, alCrear);
    } catch (error) {
        zona.innerHTML = mensajeError("No se pudo cargar el formulario de nueva edición.");
        console.error(error);
    }
}

function dibujarFormulario(
    zona: HTMLElement,
    categorias: Categoria[],
    libros: Libro[],
    alCrear: (edicion: Edicion) => void,
) {
    const opcionesLibros = libros
        .map((libro) => `<option value="${libro.id}">${escapar(libro.titulo)}</option>`)
        .join("");

    zona.innerHTML = `
    <h2>Nueva edición</h2>
    <form id="form-nueva-edicion" novalidate>
      <label for="ne-nombre">Nombre</label>
      <input id="ne-nombre" maxlength="50" placeholder="Ej.: Diciembre 2026" required />

      <label for="ne-corte">Fecha de corte</label>
      <input id="ne-corte" type="date" required />

      <label for="ne-desde">Despacho desde</label>
      <input id="ne-desde" type="date" required />

      <label for="ne-hasta">Despacho hasta</label>
      <input id="ne-hasta" type="date" required />

      <label for="ne-precio">Precio de la caja (el mismo para las 4 temáticas)</label>
      <input id="ne-precio" type="number" min="1" step="0.01" required />

      <fieldset>
        <legend>Libro del mes por temática</legend>
        ${categorias.map((categoria) => `
          <div class="curaduria-form">
            <strong>${escapar(categoria.nombre)}</strong>
            <label for="ne-libro-${categoria.id}">Libro</label>
            <select id="ne-libro-${categoria.id}" required>
              <option value="">Elegí un libro</option>
              ${opcionesLibros}
            </select>
            <label for="ne-cupo-${categoria.id}">Cupo máximo (vacío = sin límite)</label>
            <input id="ne-cupo-${categoria.id}" type="number" min="1" step="1" />
          </div>
        `).join("")}
      </fieldset>

      <button type="submit">Crear edición</button>
      <div id="ne-resultado" aria-live="polite"></div>
    </form>
  `;

    const form = zona.querySelector<HTMLFormElement>("#form-nueva-edicion")!;
    const resultado = zona.querySelector<HTMLDivElement>("#ne-resultado")!;
    const boton = form.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    const valor = (id: string) =>
        form.querySelector<HTMLInputElement | HTMLSelectElement>(`#${id}`)!.value.trim();

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        resultado.innerHTML = "";

        const nombre = valor("ne-nombre");
        const corte = valor("ne-corte");
        const desde = valor("ne-desde");
        const hasta = valor("ne-hasta");
        const precio = Number(valor("ne-precio"));

        // Precio unico: el mismo valor va en las 4 curadurias.
        const curadurias: CuraduriaNueva[] = categorias.map((categoria) => {
            const cupo = valor(`ne-cupo-${categoria.id}`);
            return {
                categoriaId: categoria.id,
                libroId: Number(valor(`ne-libro-${categoria.id}`)) || null,
                precioVigente: precio,
                cupoMaximo: cupo === "" ? null : Number(cupo),
            };
        });

        const problema = validar(nombre, corte, desde, hasta, precio, curadurias);
        if (problema) {
            resultado.innerHTML = mensajeError(problema);
            return;
        }

        boton.disabled = true;
        boton.textContent = "Creando...";
        try {
            const edicion = await apiPost<Edicion>("/ediciones", {
                nombre,
                fechaCorte: corte,
                fechaDespachoDesde: desde,
                fechaDespachoHasta: hasta,
                curadurias,
            });
            alCrear(edicion);
        } catch (error) {
            // El backend vuelve a validar: si rechaza algo (ej. nombre repetido), mostramos su mensaje.
            resultado.innerHTML = mensajeError(
                error instanceof Error ? error.message : "No se pudo crear la edición.");
            boton.disabled = false;
            boton.textContent = "Crear edición";
            console.error(error);
        }
    });
}

/** Las mismas reglas que el backend, para avisar antes de mandar. Devuelve el problema o null. */
function validar(
    nombre: string,
    corte: string,
    desde: string,
    hasta: string,
    precio: number,
    curadurias: CuraduriaNueva[],
): string | null {
    if (!nombre) return "Completá el nombre de la edición.";
    if (!corte || !desde || !hasta) return "Completá la fecha de corte y las de despacho.";
    // Las fechas vienen como "AAAA-MM-DD", asi que se pueden comparar como texto.
    if (desde <= corte) return "El despacho tiene que empezar después de la fecha de corte.";
    if (hasta < desde) return "La fecha 'despacho hasta' no puede ser anterior a 'despacho desde'.";
    if (!(precio > 0)) return "El precio tiene que ser mayor a cero.";
    if (curadurias.some((c) => c.libroId === null)) return "Elegí un libro para cada temática.";
    if (curadurias.some((c) => c.cupoMaximo !== null && !(Number.isInteger(c.cupoMaximo) && c.cupoMaximo > 0))) {
        return "El cupo, si lo cargás, tiene que ser un número entero mayor a cero.";
    }
    return null;
}