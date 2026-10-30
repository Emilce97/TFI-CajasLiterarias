type Tema = "claro" | "oscuro";

const CLAVE = "tema";
const preferenciaOscura = window.matchMedia("(prefers-color-scheme: dark)");

function leerGuardado(): Tema | null {
    try {
        const valor = localStorage.getItem(CLAVE);
        return valor === "claro" || valor === "oscuro" ? valor : null;
    } catch {
        return null; // el navegador puede tener el almacenamiento bloqueado
    }
}

function aplicar(tema: Tema): void {
    document.documentElement.dataset.tema = tema;
}

function temaDelSistema(): Tema {
    return preferenciaOscura.matches ? "oscuro" : "claro";
}

/**
 * Arranca con el tema que eligio la persona o, si nunca eligio, con el del sistema.
 * Mientras no elija, acompaña los cambios del sistema. Se llama una sola vez por pagina.
 */
export function iniciarTema(): void {
    aplicar(leerGuardado() ?? temaDelSistema());

    preferenciaOscura.addEventListener("change", () => {
        if (!leerGuardado()) aplicar(temaDelSistema());
    });

    // El boton lo dibuja el encabezado despues: un listener global alcanza para todas las pantallas.
    document.addEventListener("click", (event) => {
        if (!(event.target instanceof Element) || !event.target.closest(".tema-toggle")) return;

        const nuevo: Tema = document.documentElement.dataset.tema === "oscuro" ? "claro" : "oscuro";
        aplicar(nuevo);
        try {
            localStorage.setItem(CLAVE, nuevo);
        } catch {
            // Sin almacenamiento: el cambio vale solo para esta visita.
        }
    });
}