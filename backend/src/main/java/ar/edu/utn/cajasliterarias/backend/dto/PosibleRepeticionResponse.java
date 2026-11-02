package ar.edu.utn.cajasliterarias.backend.dto;

/**
 * Aviso antes del corte: una suscriptora activa que recibiria un libro que ya recibio
 * en una edicion anterior.
 */
public record PosibleRepeticionResponse(
        Long suscripcionId,
        String suscriptora,
        String email,
        String categoria,
        String libro) {
}