/**
 * Pie de pagina de todas las pantallas. Los estilos van en style.css (.pie).
 */
export function footer(): string {
    return `
    <footer class="footer">
      <p class="footer-marca">Cajas Literarias</p>
      <p class="footer-texto">Trabajo Final Integrador · Tecnicatura Universitaria en Programación · UTN</p>
      <p class="footer-copy">© ${new Date().getFullYear()}</p>
    </footer>
  `;
}