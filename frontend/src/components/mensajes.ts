import { escapar } from "../utils/html";

// Mensajes de estado que comparten todas las pantallas. Cada uno lleva una clase CSS
// para que se vean igual en todo el sitio: los estilos van una sola vez en style.css.

export function mensajeCargando(texto = "Cargando..."): string {
    return `<p class="mensaje mensaje-cargando">${escapar(texto)}</p>`;
}

export function mensajeError(texto: string): string {
    return `<p class="mensaje mensaje-error" role="alert">${escapar(texto)}</p>`;
}

export function mensajeVacio(texto: string): string {
    return `<p class="mensaje mensaje-vacio">${escapar(texto)}</p>`;
}

export function mensajeExito(texto: string): string {
    return `<p class="mensaje mensaje-exito" role="status">${escapar(texto)}</p>`;
}