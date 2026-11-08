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

const ICONO_HAMBURGUESA = `
  <svg class="icono-menu" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
       stroke-linecap="round" aria-hidden="true">
    <path d="M4 6h16M4 12h16M4 18h16" />
  </svg>`;

const ICONO_CERRAR = `
  <svg class="icono-cerrar" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
       stroke-linecap="round" aria-hidden="true">
    <path d="M6 6l12 12M18 6L6 18" />
  </svg>`;

// Igual que el tema: un listener global alcanza para todas las pantallas.
document.addEventListener("click", (event) => {
    if (!(event.target instanceof Element)) return;

    const cabecera = event.target.closest<HTMLElement>(".encabezado");
    if (!cabecera) return;

    const boton = cabecera.querySelector<HTMLButtonElement>(".menu-toggle");
    if (!boton) return;

    if (event.target.closest(".menu-toggle")) {
        const abierto = cabecera.classList.toggle("menu-abierto");
        boton.setAttribute("aria-expanded", String(abierto));
    } else if (event.target.closest("nav button, nav a")) {
        // Al elegir una opcion el menu se cierra
        cabecera.classList.remove("menu-abierto");
        boton.setAttribute("aria-expanded", "false");
    }
});

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
      ${menu ? `<button type="button" class="menu-toggle" aria-label="Abrir menú" aria-expanded="false">${ICONO_HAMBURGUESA}${ICONO_CERRAR}</button>` : ""}
    </header>
  `;
}