package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;

/**
 * Datos que envia el suscriptor (con sesion iniciada) para darse de alta en una
 * categoria/tematica dentro de la ventana habilitada (dia 1 al 20).
 * Nombre, email y direccion ya no viajan acá: el suscriptor sale del token.
 */
@Data
public class CrearSuscripcionRequest {

    private Long categoriaId;

}