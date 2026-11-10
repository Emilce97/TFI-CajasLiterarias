// La Caja Sorpresa no revela su libro hasta que la caja llega a la suscriptora.
// La administradora siempre lo ve: lo necesita para la demanda y el despacho.

export const TEXTO_SORPRESA = "Es una sorpresa… ¿qué libro será?";

export function esCategoriaSorpresa(nombre: string): boolean {
    return nombre.toLowerCase().includes("sorpresa");
}