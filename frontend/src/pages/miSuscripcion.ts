import { apiGet, apiPatch } from "../data/apiClient";
import { ejecutarConBoton } from "../components/botonAccion";
import { mensajeCargando, mensajeError, mensajeExito, mensajeVacio } from "../components/mensajes";
import type { CajaRecibida, Categoria, EstadoPedido, Suscripcion, Suscriptor } from "../types";
import { exigirRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { renderPagarPage } from "./pagar";

// Cómo se le muestra a el suscriptor el estado de cada caja (el panel de despacho usa los nombres internos).
const ETIQUETA_CAJA: Record<EstadoPedido, string> = {
    PENDIENTE_DE_EMPAQUE: "En preparación",
    EMPAQUETADO: "Empaquetada",
    DESPACHADO: "Despachada",
    ENTREGADO: "Entregada",
};

/**
 * "Mi suscripcion": las suscripciones de la persona logueada, con sus acciones
 * (pagar, pausar, reanudar, dar de baja, cambiar de tematica), y el formulario para
 * actualizar nombre y direccion de entrega (rige para los proximos pedidos).
 */
export async function renderMiSuscripcionPage(container: HTMLElement) {
    if (!exigirRol("SUSCRIPTOR")) return;
    container.innerHTML = "<h1>Mi suscripción</h1>" + mensajeCargando();

    try {
        const [suscripciones, categorias, perfil, cajas] = await Promise.all([
            apiGet<Suscripcion[]>("/suscripciones/mis-suscripciones"),
            apiGet<Categoria[]>("/categorias"),
            apiGet<Suscriptor>("/suscriptores/mi-perfil"),
            apiGet<CajaRecibida[]>("/suscriptores/mis-cajas"),
        ]);
        renderPagina(container, suscripciones, categorias, perfil, cajas);
    } catch (error) {
        container.innerHTML = "<h1>Mi suscripción</h1>" + mensajeError("No se pudo cargar tu información");
        console.error(error);
    }
}

function renderPagina(
    container: HTMLElement,
    suscripciones: Suscripcion[],
    categorias: Categoria[],
    perfil: Suscriptor,
    cajas: CajaRecibida[],
) {
    container.innerHTML = `
    <h1>Mi suscripción</h1>
    <section id="mis-suscripciones">
      ${suscripciones.length === 0
            ? mensajeVacio("Todavía no tenés suscripciones.") + '<p><a class="boton" href="/suscripcion.html">Suscribirme</a></p>'
            : `<ul>${suscripciones.map((s) => renderSuscripcion(s, categorias)).join("")}</ul>`}
    </section>

    <h2>Mis cajas recibidas</h2>
    <section id="mis-cajas">
      ${cajas.length === 0
            ? mensajeVacio("Todavía no tenés cajas. Cuando se cierre una edición con tu pago validado, la vas a ver acá.")
            : renderCajas(cajas)}
    </section>

    <h2>Mis datos de entrega</h2>
    <form id="form-perfil" novalidate>
      <label for="perfil-nombre">Nombre y apellido</label>
      <input id="perfil-nombre" value="${escapar(perfil.nombre)}" maxlength="150" required />
      <label for="perfil-direccion">Dirección de entrega</label>
      <input id="perfil-direccion" value="${escapar(perfil.direccion ?? "")}" maxlength="255" required />
      <button type="submit">Guardar</button>
    </form>
    <div id="mensaje-perfil" aria-live="polite"></div>
  `;

    conectarAcciones(container);
    conectarPerfil(container);
}

function boton(id: number, accion: string, texto: string): string {
    return `<button data-accion="${accion}" data-id="${id}">${texto}</button>`;
}

function renderSuscripcion(s: Suscripcion, categorias: Categoria[]): string {
    const pendientes: string[] = [];
    if (s.proximoEstado) {
        pendientes.push(`pasa a ${escapar(s.proximoEstado)} desde la próxima edición`);
    }
    if (s.proximaCategoria) {
        pendientes.push(`cambia a ${escapar(s.proximaCategoria.nombre)} desde la próxima edición`);
    }

    const acciones: string[] = [];
    if (s.estado === "ACTIVA") {
        acciones.push(boton(s.id, "pagar", "Pagar"));
        acciones.push(boton(s.id, "pausa", "Pausar"));
    }
    if (s.estado === "PAUSADA") acciones.push(boton(s.id, "reanudar", "Reanudar"));
    if (s.estado !== "BAJA") acciones.push(boton(s.id, "baja", "Dar de baja"));

    const cambioDeCategoria = s.estado === "ACTIVA"
        ? `<select data-categoria-de="${s.id}">
             ${categorias
            .filter((c) => c.id !== s.categoria.id)
            .map((c) => `<option value="${c.id}">${escapar(c.nombre)}</option>`)
            .join("")}
           </select>
           ${boton(s.id, "categoria", "Cambiar temática")}`
        : "";

    return `
    <li>
      <strong>${escapar(s.categoria.nombre)}</strong>
      <span class="etiqueta etiqueta-${escapar(s.estado.toLowerCase())}">${escapar(s.estado)}</span>
      ${pendientes.length > 0 ? `<br><em>Pendiente: ${pendientes.join("; ")}.</em>` : ""}
      <div>${acciones.join(" ")} ${cambioDeCategoria}</div>
    </li>
  `;
}

function renderCajas(cajas: CajaRecibida[]): string {
    return `
    <table>
      <thead>
        <tr>
          <th>Edición</th>
          <th>Temática</th>
          <th>Libro</th>
          <th>Estado</th>
          <th>Seguimiento</th>
        </tr>
      </thead>
      <tbody>
        ${cajas.map((c) => `
        <tr>
          <td>${escapar(c.edicion)}</td>
          <td>${escapar(c.categoria)}</td>
          <td>${escapar(c.libro)}</td>
          <td><span class="etiqueta">${ETIQUETA_CAJA[c.estado]}</span></td>
          <td>${escapar(c.tracking) || "—"}</td>
        </tr>`).join("")}
      </tbody>
    </table>
  `;
}

function conectarAcciones(container: HTMLElement) {
    const lista = container.querySelector<HTMLElement>("#mis-suscripciones")!;

    // Un solo listener para todos los botones (delegacion de eventos).
    lista.addEventListener("click", async (event) => {
        const clickeado = (event.target as HTMLElement).closest<HTMLButtonElement>("button[data-accion]");
        if (!clickeado) return;

        const id = clickeado.dataset.id;
        const accion = clickeado.dataset.accion!;
        if (accion === "baja" && !confirm("¿Seguro que querés dar de baja esta suscripción?")) return;

        // "Pagar" no llama al backend desde aca: lleva a la pantalla de pago.
        if (accion === "pagar") {
            await renderPagarPage(container, Number(id));
            return;
        }

        // Sin texto fijo: si falla, el alert muestra el mensaje del backend.
        const ok = await ejecutarConBoton(clickeado, "Procesando...", async () => {
            if (accion === "categoria") {
                const select = lista.querySelector<HTMLSelectElement>(`select[data-categoria-de="${id}"]`)!;
                await apiPatch(`/suscripciones/${id}/categoria`, { categoriaId: Number(select.value) });
            } else {
                await apiPatch(`/suscripciones/${id}/${accion}`);
            }
        });
        // Recargamos para ver el estado nuevo (o el cambio pendiente).
        if (ok) await renderMiSuscripcionPage(container);
    });
}

function conectarPerfil(container: HTMLElement) {
    const form = container.querySelector<HTMLFormElement>("#form-perfil")!;
    const mensaje = container.querySelector<HTMLElement>("#mensaje-perfil")!;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const boton = form.querySelector<HTMLButtonElement>('button[type="submit"]')!;
        const nombre = form.querySelector<HTMLInputElement>("#perfil-nombre")!.value.trim();
        const direccion = form.querySelector<HTMLInputElement>("#perfil-direccion")!.value.trim();
        if (!nombre || !direccion) {
            mensaje.innerHTML = mensajeError("Completá nombre y dirección.");
            return;
        }
        mensaje.innerHTML = "";

        await ejecutarConBoton(
            boton,
            "Guardando...",
            async () => {
                await apiPatch("/suscriptores/mi-perfil", { nombre, direccion });
                mensaje.innerHTML = mensajeExito("Datos actualizados. Se aplican a los próximos pedidos.");
            },
            undefined, // se muestra el mensaje del backend
            (texto) => { mensaje.innerHTML = mensajeError(texto); },
        );
    });
}