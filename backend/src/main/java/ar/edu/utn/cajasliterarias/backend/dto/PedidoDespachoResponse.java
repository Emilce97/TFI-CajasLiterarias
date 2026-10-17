package ar.edu.utn.cajasliterarias.backend.dto;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;

import java.time.LocalDateTime;

/**
 * Lo que ve el panel de despacho de cada pedido. No se devuelve la entidad
 * para no exponer sus relaciones (suscripción, pago, administradora con su hash).
 */
public record PedidoDespachoResponse(
        Long id,
        String nombreDestinatario,
        String direccionEntrega,
        String categoria,
        String libro,
        EstadoPedido estado,
        String tracking,
        String observaciones,
        LocalDateTime fechaModificacion,
        String modificadoPor) {

    public static PedidoDespachoResponse desde(PedidoEdicion p) {
        return new PedidoDespachoResponse(
                p.getId(),
                p.getNombreDestinatario(),
                p.getDireccionEntrega(),
                p.getCategoriaCongelada(),
                p.getLibroCongelado(),
                p.getEstadoPedido(),
                p.getTracking(),
                p.getObservaciones(),
                p.getFechaModificacion(),
                p.getModificadoPor() != null ? p.getModificadoPor().getEmail() : null);
    }
}