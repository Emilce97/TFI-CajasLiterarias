/**
 * Ejecuta la accion de un boton (validar, guardar, avanzar un estado...).
 * Mientras corre, deshabilita el boton y muestra un texto de espera, asi no se
 * manda dos veces. Si falla, avisa con un mensaje. En los dos casos el boton
 * vuelve a como estaba, para poder reintentar.
 * Devuelve true si la accion salio bien.
 */
export async function ejecutarConBoton(
    boton: HTMLButtonElement,
    textoOcupado: string,
    accion: () => Promise<unknown>,
    mensajeSiFalla: string,
): Promise<boolean> {
    const textoOriginal = boton.textContent ?? "";
    boton.disabled = true;
    boton.textContent = textoOcupado;

    try {
        await accion();
        return true;
    } catch (error) {
        alert(mensajeSiFalla);
        console.error(error);
        return false;
    } finally {
        boton.disabled = false;
        boton.textContent = textoOriginal;
    }
}