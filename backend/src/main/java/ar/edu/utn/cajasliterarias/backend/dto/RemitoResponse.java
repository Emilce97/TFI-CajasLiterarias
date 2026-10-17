package ar.edu.utn.cajasliterarias.backend.dto;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import lombok.Data;

import java.time.LocalDate;

/**
 * Remito de un pedido. Todos los datos salen del snapshot de PedidoEdicion (congelados en el corte).
 */
@Data
public class RemitoResponse {

    private Long numero;
    private LocalDate fechaEmision;
    private String edicion;
    private String nombreDestinatario;
    private String direccionEntrega;
    private String categoria;
    private String libro;
    private EstadoPedido estado;
    private String tracking;
}
