// El nombre y la direccion los cargan los suscriptores en el formulario publico:
// se escapan para que un texto con etiquetas HTML no se ejecute en pantalla.
export function escapar(texto: string | null): string {
    return (texto ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}