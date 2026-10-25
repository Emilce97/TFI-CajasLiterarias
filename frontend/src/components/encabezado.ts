/**
 * Encabezado de todas las pantallas: el nombre del sitio (con link a la pagina principal)
 * y, si se pasa, el menu de navegacion. 
 */
export function encabezado(menu = ""): string {
    return `
    <header class="encabezado">
      <a class="marca" href="/">Cajas Literarias</a>
      ${menu}
    </header>
  `;
}