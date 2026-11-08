/**
 * Para que las tablas se vean como tarjetas en pantallas chicas, cada celda necesita
 * saber a que columna pertenece. Copiamos el texto de cada <th> en el atributo
 * data-label de las celdas de su columna; el CSS lo muestra con ::before.
 */
function etiquetar(tabla: HTMLTableElement): void {
    const titulos = Array.from(tabla.querySelectorAll("thead th")).map(
        (th) => th.textContent?.trim() ?? "",
    );

    tabla.querySelectorAll("tbody tr").forEach((fila) => {
        fila.querySelectorAll("td").forEach((celda, i) => {
            if (titulos[i]) celda.dataset.label = titulos[i];
        });
    });
    tabla.dataset.etiquetada = "si";
}

/**
 * Las tablas se dibujan despues de pedir datos al backend, asi que no existen al cargar la pagina.
 * Un MutationObserver avisa cada vez que cambia el contenido y etiqueta las tablas nuevas.
 */
export function etiquetarTablas(raiz: HTMLElement): void {
    const observador = new MutationObserver(() => {
        raiz
            .querySelectorAll<HTMLTableElement>("table:not([data-etiquetada])")
            .forEach(etiquetar);
    });
    observador.observe(raiz, { childList: true, subtree: true });
}