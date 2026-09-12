package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;

/**
 * Datos que envia el suscriptor (ya registrado) para darse de alta
 * en una categoria/tematica dentro de la ventana habilitada (dia 1 al 20).
 */
@Data
public class CrearSuscripcionRequest {

    private Long suscriptorId;
    private Long categoriaId;

}