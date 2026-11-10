import { apiGet, apiPost } from "../data/apiClient";
import type { Libro, Proveedor } from "../types";
import { escapar } from "../utils/html";
import { mensajeCargando, mensajeError, mensajeExito, mensajeVacio } from "../components/mensajes";

/**
 * Pagina "Catalogo" (solo ADMIN): alta y listado de proveedores y libros.
 * Las categorias no tienen pantalla: son 4 fijas y se cargan con datos-iniciales.sql.
 * Fuera de alcance: editar o borrar.
 */
export async function renderCatalogoPage(container: HTMLElement, aviso?: string) {
    container.innerHTML = "<h1>Catálogo</h1>" + mensajeCargando("Cargando catálogo...");

    try {
        const [proveedores, libros] = await Promise.all([
            apiGet<Proveedor[]>("/proveedores"),
            apiGet<Libro[]>("/libros"),
        ]);
        dibujar(container, proveedores, libros, aviso);
    } catch (error) {
        container.innerHTML = "<h1>Catálogo</h1>" + mensajeError("No se pudo cargar el catálogo.");
        console.error(error);
    }
}

function dibujar(container: HTMLElement, proveedores: Proveedor[], libros: Libro[], aviso?: string) {
    const opcionesProveedor = proveedores
        .map((p) => `<option value="${p.id}">${escapar(p.nombre)}</option>`)
        .join("");

    container.innerHTML = `
    <h1>Catálogo</h1>
    ${aviso ? mensajeExito(aviso) : ""}

    <section id="catalogo-proveedores">
      <h2>Proveedores</h2>
      <form id="form-proveedor" novalidate>
        <label for="pr-nombre">Nombre</label>
        <input id="pr-nombre" maxlength="150" required />

        <label for="pr-contacto">Contacto (opcional)</label>
        <input id="pr-contacto" maxlength="150" placeholder="Email o teléfono" />

        <button type="submit">Agregar proveedor</button>
        <div id="pr-resultado" aria-live="polite"></div>
      </form>
      ${tablaProveedores(proveedores)}
    </section>

    <section id="catalogo-libros">
      <h2>Libros</h2>
      <form id="form-libro" novalidate>
        <label for="li-titulo">Título</label>
        <input id="li-titulo" maxlength="255" required />

        <label for="li-autor">Autor (opcional)</label>
        <input id="li-autor" maxlength="150" />

        <label for="li-proveedor">Proveedor (opcional)</label>
        <select id="li-proveedor">
          <option value="">Sin proveedor</option>
          ${opcionesProveedor}
        </select>

        <button type="submit">Agregar libro</button>
        <div id="li-resultado" aria-live="polite"></div>
      </form>
      ${tablaLibros(libros)}
    </section>
  `;

    conectarFormulario(container, "#form-proveedor", "#pr-resultado", "Agregar proveedor", () => {
        const nombre = valor(container, "#pr-nombre");
        if (!nombre) return { problema: "Completá el nombre del proveedor." };
        return {
            ruta: "/proveedores",
            cuerpo: { nombre, contacto: valor(container, "#pr-contacto") || null },
            exito: "Proveedor agregado al catálogo.",
        };
    });

    conectarFormulario(container, "#form-libro", "#li-resultado", "Agregar libro", () => {
        const titulo = valor(container, "#li-titulo");
        if (!titulo) return { problema: "Completá el título del libro." };
        return {
            ruta: "/libros",
            cuerpo: {
                titulo,
                autor: valor(container, "#li-autor") || null,
                // "" (Sin proveedor) -> Number("") es 0 -> || null lo convierte en "sin proveedor".
                proveedorId: Number(valor(container, "#li-proveedor")) || null,
            },
            exito: "Libro agregado al catálogo.",
        };
    });
}

/** Lo que arma cada formulario antes de enviar: un problema para mostrar, o los datos del POST. */
type Envio =
    | { problema: string }
    | { ruta: string; cuerpo: unknown; exito: string };

/**
 * Comportamiento comun de los dos formularios: validar, enviar, y recargar la pagina con un aviso
 * si sale bien; si el backend rechaza (ej. nombre repetido), mostrar su mensaje.
 */
function conectarFormulario(
    container: HTMLElement,
    selectorForm: string,
    selectorResultado: string,
    textoBoton: string,
    armarEnvio: () => Envio,
) {
    const form = container.querySelector<HTMLFormElement>(selectorForm)!;
    const resultado = container.querySelector<HTMLDivElement>(selectorResultado)!;
    const boton = form.querySelector<HTMLButtonElement>('button[type="submit"]')!;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        resultado.innerHTML = "";

        const envio = armarEnvio();
        if ("problema" in envio) {
            resultado.innerHTML = mensajeError(envio.problema);
            return;
        }

        boton.disabled = true;
        boton.textContent = "Guardando...";
        try {
            await apiPost(envio.ruta, envio.cuerpo);
            renderCatalogoPage(container, envio.exito);
        } catch (error) {
            resultado.innerHTML = mensajeError(
                error instanceof Error ? error.message : "No se pudo guardar.");
            boton.disabled = false;
            boton.textContent = textoBoton;
            console.error(error);
        }
    });
}

function valor(container: HTMLElement, selector: string): string {
    return container.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!.value.trim();
}

function tablaProveedores(proveedores: Proveedor[]): string {
    if (proveedores.length === 0) return mensajeVacio("Todavía no hay proveedores cargados.");
    return `
    <table>
      <thead><tr><th>Nombre</th><th>Contacto</th></tr></thead>
      <tbody>
        ${proveedores.map((p) => `
          <tr>
            <td>${escapar(p.nombre)}</td>
            <td>${p.contacto ? escapar(p.contacto) : "—"}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function tablaLibros(libros: Libro[]): string {
    if (libros.length === 0) return mensajeVacio("Todavía no hay libros cargados.");
    return `
    <table>
      <thead><tr><th>Título</th><th>Autor</th><th>Proveedor</th></tr></thead>
      <tbody>
        ${libros.map((l) => `
          <tr>
            <td>${escapar(l.titulo)}</td>
            <td>${l.autor ? escapar(l.autor) : "—"}</td>
            <td>${l.proveedor ? escapar(l.proveedor.nombre) : "—"}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}