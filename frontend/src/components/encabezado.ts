import { iniciarTema } from "../utils/tema";

// El boton de tema vive en el encabezado, asi que el encabezado se ocupa de iniciarlo.
iniciarTema();

const ICONO_SOL = `
  <svg class="icono-sol" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
       stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
  </svg>`;

const ICONO_LUNA = `
  <svg class="icono-luna" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
       stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
  </svg>`;

/**
 * Encabezado de todas las pantallas: el nombre del sitio (con link a la pagina principal),
 * si se pasa el menu de navegacion, y el boton para cambiar entre tema claro y oscuro.
 * Los estilos van en style.css.
 */
export function encabezado(menu = ""): string {
    return `
    <header class="encabezado">
      <a class="marca" href="/">Cajas Literarias</a>
      ${menu}
      <button type="button" class="tema-toggle" aria-label="Cambiar entre tema claro y oscuro" title="Cambiar tema">
        ${ICONO_SOL}${ICONO_LUNA}
      </button>
    </header>
  `;
}