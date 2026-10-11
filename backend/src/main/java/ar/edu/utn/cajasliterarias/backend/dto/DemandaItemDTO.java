package ar.edu.utn.cajasliterarias.backend.dto;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoFaltante;
import lombok.Data;

/**
 * Una fila de la demanda de una edicion: cuantos ejemplares de un libro hacen falta,
 * cuantos llegaron del proveedor y cuantos faltan.
 */
@Data
public class DemandaItemDTO {

    private Long id;
    private Long libroId;
    private String titulo;
    private String autor;
    private Long proveedorId;
    private String proveedorNombre;
    private int cantidadRequerida;
    private int cantidadRecibida;
    // Calculado: requerida - recibida (nunca negativo).
    private int cantidadFaltante;
    private EstadoFaltante estadoFaltante;
}