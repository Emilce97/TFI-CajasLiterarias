package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Resumen que se devuelve al finalizar el corte de una edicion:
 * cuantos pedidos se generaron y cuales suscripciones quedaron excluidas.
 */
@Data
public class ResumenCierreDTO {

    private Long edicionId;
    private int totalPedidosGenerados;
    private int totalExcluidos;
    private List<Long> suscripcionesExcluidas;
}