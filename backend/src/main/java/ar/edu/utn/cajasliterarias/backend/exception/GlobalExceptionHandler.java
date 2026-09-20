package ar.edu.utn.cajasliterarias.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Centraliza el manejo de excepciones de negocio para toda la API,
 * devolviendo respuestas HTTP consistentes en vez de que cada controller
 * tenga su propio try/catch repetido.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EdicionYaCerradaException.class)
    public ResponseEntity<Map<String, String>> manejarEdicionYaCerrada(EdicionYaCerradaException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409: la edicion ya fue cerrada, hay un conflicto de estado
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400: datos invalidos en el pedido
                .body(Map.of("error", e.getMessage()));
    }
}