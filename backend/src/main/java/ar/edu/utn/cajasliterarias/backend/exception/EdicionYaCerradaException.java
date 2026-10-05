package ar.edu.utn.cajasliterarias.backend.exception;

/**
 * El corte no puede ejecutarse sobre una edicion que ya no esta ABIERTA (ya fue cerrada),
 * ni sobre una edicion todavia ABIERTA que ya tiene PedidoEdicion generados (estado inconsistente:
 * el proceso de corte se interrumpio despues de generar los pedidos pero antes de marcar CERRADA).
 * Se responde 409.
 */
public class EdicionYaCerradaException extends RuntimeException {

    public EdicionYaCerradaException(Long edicionId) {
        super("La edicion con id " + edicionId + " ya fue cerrada; no puede reprocesarse el corte.");
    }

}