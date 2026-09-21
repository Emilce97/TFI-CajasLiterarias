package ar.edu.utn.cajasliterarias.backend.exception;

/**
 * Guarda defensiva: salta si una edicion todavia ABIERTA ya tiene PedidoEdicion generados, un estado inconsistente
 * (el proceso de corte se interrumpio despues de generar los pedidos pero antes de marcar CERRADA).
 * El reintento normal de un corte ya finalizado no llega aca: corta antes con IllegalStateException,
 * al no encontrar edicion ABIERTA.
 */
public class EdicionYaCerradaException extends RuntimeException {

    public EdicionYaCerradaException(Long edicionId) {
        super("La edicion con id " + edicionId + " ya fue cerrada; no puede reprocesarse el corte.");
    }

}