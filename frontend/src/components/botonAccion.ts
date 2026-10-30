/**
 * Ejecuta la accion de un boton (validar, guardar, avanzar un estado...).
 * Deshabilita el boton mientras corre, asi no se manda dos veces, y lo deja
 * como estaba al terminar. Devuelve true si la accion salio bien.
 *
 * Si falla avisa con alert(), o con alFallar si se pasa (para mostrarlo en pantalla).
 * El mensaje es mensajeSiFalla o, si se omite, el que mando el backend.
 */
export async function ejecutarConBoton(
    boton: HTMLButtonElement,
    textoOcupado: string,
    accion: () => Promise<unknown>,
    mensajeSiFalla?: string,
    alFallar?: (mensaje: string) => void,
): Promise<boolean> {
    const textoOriginal = boton.textContent ?? "";
    boton.disabled = true;
    boton.textContent = textoOcupado;

    try {
        await accion();
        return true;
    } catch (error) {
        const mensaje = mensajeSiFalla
            ?? (error instanceof Error ? error.message : "Ocurrió un error inesperado.");
        if (alFallar) {
            alFallar(mensaje);
        } else {
            alert(mensaje);
        }
        console.error(error);
        return false;
    } finally {
        boton.disabled = false;
        boton.textContent = textoOriginal;
    }
}