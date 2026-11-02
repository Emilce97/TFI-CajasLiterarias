package ar.edu.utn.cajasliterarias.backend.dto;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;

/**
 * Lo que ve la suscriptora de cada pedido suyo en "Mis cajas recibidas". Es un DTO a propósito:
 * no se devuelve la entidad para no exponer el pago, la administradora ni la suscripción.
 */
public record CajaRecibidaResponse(
        Long id,
        String edicion,
        String categoria,
        String libro,
        EstadoPedido estado,
        String tracking) {

    public static CajaRecibidaResponse desde(PedidoEdicion p) {
        return new CajaRecibidaResponse(
                p.getId(),
                p.getEdicion().getNombre(),
                p.getCategoriaCongelada(),
                p.getLibroCongelado(),
                p.getEstadoPedido(),
                p.getTracking());
    }
}
