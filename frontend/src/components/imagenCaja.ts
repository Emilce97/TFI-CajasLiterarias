/**
 * Foto de cada tematica (archivos en public/cajas/). Es la misma en todas las ediciones
 * porque depende solo del nombre de la categoria. Si aparece una categoria que no
 * esta en la lista, se usa la de Caja Sorpresa.
 */
const FOTOS = [
    { palabras: ["misterio", "terror"], archivo: "misterio" },
    { palabras: ["narrativa", "drama"], archivo: "narrativa" },
    { palabras: ["romance"], archivo: "romance" },
    { palabras: ["sorpresa"], archivo: "sorpresa" },
];

export function imagenDeCategoria(nombre: string): string {
    const buscado = nombre.toLowerCase();
    const foto = FOTOS.find((f) => f.palabras.some((palabra) => buscado.includes(palabra)));
    return `/cajas/${foto?.archivo ?? "sorpresa"}.jpg`;
}