// Escapa texto de usuarios antes de meterlo en innerHTML (evita XSS).
export function esc(texto: string): string {
    return texto.replace(/[&<>"']/g, (c) =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);
}