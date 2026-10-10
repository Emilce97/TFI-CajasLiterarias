package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;

/**
 * Cuerpo de PATCH /api/demanda/{id}/recepcion.
 * cantidadRecibida es el TOTAL recibido hasta ahora (reemplaza al valor anterior, no se suma),
 * asi un error de carga se corrige escribiendo el numero correcto.
 */
@Data
public class RegistrarRecepcionRequest {

    private Integer cantidadRecibida;
}