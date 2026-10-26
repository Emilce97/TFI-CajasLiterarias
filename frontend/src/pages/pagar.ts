import { apiGet, apiPost } from "../data/apiClient";
import type { CuraduriaEdicion, Edicion, Pago, Suscripcion } from "../types";
import { exigirRol } from "../utils/auth";
import { escapar } from "../utils/html";
import { formatearFecha, formatearMonto } from "../utils/formato";
import { mensajeCargando, mensajeError, mensajeExito, mensajeVacio } from "../components/mensajes";
import { renderMiSuscripcionPage } from "./miSuscripcion";
import { renderEdicionesPage } from "./ediciones";

/**
 * "Pagar": la suscriptora informa el pago de la edicion abierta para una de sus suscripciones.
 * El pago es por transferencia y la conciliacion es manual: aca se informa (queda PENDIENTE)
 * y la administradora lo valida desde el panel de pagos.
 */

// Datos de ejemplo para la transferencia (en un despliegue real vendrian de la configuracion).
const DATOS_TRANSFERENCIA = {
    titular: "Cajas Literarias",
    alias: "CAJAS.LITERARIAS",
    cbu: "0000003100000000000000",
};

const TITULO = "<h1>Pagar</h1>";

export async function renderPagarPage(container: HTMLElement, suscripcionId: number) {
    if (!exigirRol("SUSCRIPTOR")) return;
    container.innerHTML = TITULO + mensajeCargando();

    try {
        const [suscripciones, ediciones] = await Promise.all([
            apiGet<Suscripcion[]>("/suscripciones/mis-suscripciones"),
            apiGet<Edicion[]>("/ediciones"),
        ]);
        const suscripcion = suscripciones.find((s) => s.id === suscripcionId);
        const edicion = ediciones.find((e) => e.estado === "ABIERTA");

        if (!suscripcion) {
            mostrarAviso(container, "No encontramos esa suscripción.");
            return;
        }
        if (suscripcion.estado !== "ACTIVA") {
            mostrarAviso(container, "Solo se pueden pagar suscripciones activas. Si está pausada, reanudala primero.");
            return;
        }
        if (!edicion) {
            mostrarAviso(container, "No hay una edición abierta para pagar por el momento.");
            return;
        }

        const curadurias = await apiGet<CuraduriaEdicion[]>(`/ediciones/${edicion.id}/curadurias`);
        const curaduria = curadurias.find((c) => c.categoria.id === suscripcion.categoria.id);
        if (!curaduria) {
            mostrarAviso(container, "La temática de tu suscripción no está disponible en esta edición.");
            return;
        }

        renderResumen(container, suscripcion, edicion, curaduria);
    } catch (error) {
        container.innerHTML = TITULO + mensajeError("No se pudo cargar la información del pago.") + botonesVolver();
        conectarVolver(container);
        console.error(error);
    }
}

function renderResumen(
    container: HTMLElement,
    suscripcion: Suscripcion,
    edicion: Edicion,
    curaduria: CuraduriaEdicion,
) {
    container.innerHTML = `
    ${TITULO}
    <h2>Resumen</h2>
    <ul>
      <li>Edición: <strong>${escapar(edicion.nombre)}</strong> (corte: ${formatearFecha(edicion.fechaCorte)})</li>
      <li>Temática: <strong>${escapar(suscripcion.categoria.nombre)}</strong></li>
      <li>Libro: "${escapar(curaduria.libro.titulo)}"</li>
      <li>Monto: <strong>${formatearMonto(curaduria.precioVigente)}</strong></li>
    </ul>

    <h2>Datos para transferir</h2>
    <ul>
      <li>Titular: ${DATOS_TRANSFERENCIA.titular}</li>
      <li>Alias: <strong>${DATOS_TRANSFERENCIA.alias}</strong></li>
      <li>CBU: ${DATOS_TRANSFERENCIA.cbu}</li>
    </ul>
    <p>Cuando hayas hecho la transferencia, informala acá. El pago queda pendiente
       hasta que la administradora lo valide.</p>

    <button type="button" id="informar-pago">Ya transferí, informar pago</button>
    <div id="resultado-pago" aria-live="polite"></div>
    ${botonesVolver()}
  `;

    conectarVolver(container);

    const boton = container.querySelector<HTMLButtonElement>("#informar-pago")!;
    const resultado = container.querySelector<HTMLDivElement>("#resultado-pago")!;

    boton.addEventListener("click", async () => {
        boton.disabled = true;
        boton.textContent = "Informando...";
        resultado.innerHTML = "";

        try {
            await apiPost<Pago>("/pagos", {
                suscripcionId: suscripcion.id,
                edicionId: edicion.id,
                monto: curaduria.precioVigente,
            });
            // Listo: se reemplaza la pantalla por la confirmacion (sin boton para no informar dos veces).
            container.innerHTML = TITULO
                + mensajeExito("¡Pago informado! Queda pendiente hasta que la administradora lo valide.")
                + botonesVolver();
            conectarVolver(container);
        } catch (error) {
            // Ej.: ya habia un pago para esta edicion (409): se muestra el mensaje del backend.
            resultado.innerHTML = mensajeError(error instanceof Error ? error.message : "No se pudo informar el pago.");
            boton.disabled = false;
            boton.textContent = "Ya transferí, informar pago";
            console.error(error);
        }
    });
}

function mostrarAviso(container: HTMLElement, texto: string) {
    container.innerHTML = TITULO + mensajeVacio(texto) + botonesVolver();
    conectarVolver(container);
}

function botonesVolver(): string {
    return `
    <p>
      <button type="button" data-volver="mi-suscripcion">Volver a Mi suscripción</button>
      <button type="button" data-volver="inicio">Volver al inicio</button>
    </p>
  `;
}

function conectarVolver(container: HTMLElement) {
    container.querySelectorAll<HTMLButtonElement>("button[data-volver]").forEach((boton) => {
        boton.addEventListener("click", () => {
            if (boton.dataset.volver === "inicio") {
                renderEdicionesPage(container);
            } else {
                renderMiSuscripcionPage(container);
            }
        });
    });
}