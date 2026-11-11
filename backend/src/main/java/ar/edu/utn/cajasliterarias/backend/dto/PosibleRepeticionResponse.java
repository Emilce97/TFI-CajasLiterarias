package ar.edu.utn.cajasliterarias.backend.dto;

/**
 * Aviso antes del corte: una suscriptor activa que recibiria un libro que ya recibio
 * en una edicion anterior.
 */
public record PosibleRepeticionResponse(
        Long suscripcionId,
        String suscriptor,
        String email,
        String categoria,
        String libro) {
}