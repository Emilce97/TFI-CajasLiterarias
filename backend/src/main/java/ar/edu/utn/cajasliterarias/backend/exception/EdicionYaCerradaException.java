package ar.edu.utn.cajasliterarias.backend.exception;

/**
 * Se lanza cuando se intenta cerrar (cortar) una edicion que ya tiene PedidoEdicion generados.
 * Es la guarda de idempotencia del corte: evita que un reintento del proceso duplique pedidos.
 */
public class EdicionYaCerradaException extends RuntimeException {

    public EdicionYaCerradaException(Long edicionId) {
        super("La edicion con id " + edicionId + " ya fue cerrada; no puede reprocesarse el corte.");
    }

}