import { apiGet, apiPatch } from "../data/apiClient";
import type { Categoria, Suscripcion, Suscriptor } from "../types";
import { exigirRol } from "../utils/auth";
import { escapar } from "../utils/html";

/**
 * "Mi suscripcion": las suscripciones de la persona logueada, con sus acciones
 * (pausar, reanudar, dar de baja, cambiar de tematica), y el formulario para
 * actualizar nombre y direccion de entrega (rige para los proximos pedidos).
 */
export async function renderMiSuscripcionPage(container: HTMLElement) {
    if (!exigirRol("SUSCRIPTOR")) return;
    container.innerHTML = "<h1>Mi suscripción</h1><p>Cargando...</p>";

    try {
        const [suscripciones, categorias, perfil] = await Promise.all([
            apiGet<Suscripcion[]>("/suscripciones/mis-suscripciones"),
            apiGet<Categoria[]>("/categorias"),
            apiGet<Suscriptor>("/suscriptores/mi-perfil"),
        ]);
        renderPagina(container, suscripciones, categorias, perfil);
    } catch (error) {
        container.innerHTML = "<h1>Mi suscripción</h1><p>No se pudo cargar tu información.</p>";
        console.error(error);
    }
}

function renderPagina(
    container: HTMLElement,
    suscripciones: Suscripcion[],
    categorias: Categoria[],
    perfil: Suscriptor
) {
    container.innerHTML = `
    <h1>Mi suscripción</h1>
    <section id="mis-suscripciones">
      ${suscripciones.length === 0
            ? '<p>Todavía no tenés suscripciones. <a href="/suscripcion.html">Suscribirme</a></p>'
            : ""}
      <ul>${suscripciones.map((s) => renderSuscripcion(s, categorias)).join("")}</ul>
    </section>

    <h2>Mis datos de entrega</h2>
    <form id="form-perfil" novalidate>
      <label for="perfil-nombre">Nombre y apellido</label>
      <input id="perfil-nombre" value="${escapar(perfil.nombre)}" maxlength="150" required />
      <label for="perfil-direccion">Dirección de entrega</label>
      <input id="perfil-direccion" value="${escapar(perfil.direccion ?? "")}" maxlength="255" required />
      <button type="submit">Guardar</button>
    </form>
    <p id="mensaje-perfil" role="status" aria-live="polite"></p>
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
    if (s.estado === "ACTIVA") acciones.push(boton(s.id, "pausa", "Pausar"));
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
      <strong>${escapar(s.categoria.nombre)}</strong> — ${escapar(s.estado)}
      ${pendientes.length > 0 ? `<br><em>Pendiente: ${pendientes.join("; ")}.</em>` : ""}
      <div>${acciones.join(" ")} ${cambioDeCategoria}</div>
    </li>
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

        clickeado.disabled = true;
        try {
            if (accion === "categoria") {
                const select = lista.querySelector<HTMLSelectElement>(`select[data-categoria-de="${id}"]`)!;
                await apiPatch(`/suscripciones/${id}/categoria`, { categoriaId: Number(select.value) });
            } else {
                await apiPatch(`/suscripciones/${id}/${accion}`);
            }
            await renderMiSuscripcionPage(container);
        } catch (error) {
            clickeado.disabled = false;
            alert(error instanceof Error ? error.message : "No se pudo completar la operación.");
            console.error(error);
        }
    });
}

function conectarPerfil(container: HTMLElement) {
    const form = container.querySelector<HTMLFormElement>("#form-perfil")!;
    const mensaje = container.querySelector<HTMLElement>("#mensaje-perfil")!;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const nombre = form.querySelector<HTMLInputElement>("#perfil-nombre")!.value.trim();
        const direccion = form.querySelector<HTMLInputElement>("#perfil-direccion")!.value.trim();
        if (!nombre || !direccion) {
            mensaje.textContent = "Completá nombre y dirección.";
            return;
        }
        try {
            await apiPatch("/suscriptores/mi-perfil", { nombre, direccion });
            mensaje.textContent = "Datos actualizados. Se aplican a los próximos pedidos.";
        } catch (error) {
            mensaje.textContent = error instanceof Error ? error.message : "No se pudieron guardar los datos.";
        }
    });
}