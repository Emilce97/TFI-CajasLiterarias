package ar.edu.utn.cajasliterarias.backend.dto;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;

/**
 * Pedido de la administradora para avanzar un pedido al estado siguiente.
 * tracking es obligatorio al pasar a DESPACHADO; observaciones es opcional.
 */
public record CambiarEstadoPedidoRequest(
        EstadoPedido estado,
        String tracking,
        String observaciones) {
}