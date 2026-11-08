/**
 * Banner de bienvenida de la pagina de Ediciones (solo para visitantes y suscriptores).
 * La imagen de fondo se define en style.css (.banner). El boton baja hasta la lista de cajas.
 */
export function banner(): string {
    return `
    <section class="banner" aria-label="Presentación">
      <div class="banner-contenido">
        <p class="banner-titulo">Cada mes, una historia nueva en tu puerta</p>
        <p class="banner-texto">Elegí tu temática y recibí una caja con libro curado, regalos y sorpresas.</p>
        <a class="boton" href="#lista-ediciones">Ver cajas</a>
      </div>
    </section>
  `;
}