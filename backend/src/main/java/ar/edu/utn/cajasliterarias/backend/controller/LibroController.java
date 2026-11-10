package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearLibroRequest;
import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.service.LibroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogo de libros (solo ADMIN: la ruta no esta declarada como publica en SecurityConfig).
 */
@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    /** GET /api/libros: los libros para elegir la curaduria de una edicion nueva. */
    @GetMapping
    public List<Libro> listar() {
        return libroService.listarLibros();
    }
    /** POST /api/libros: alta de un libro en el catalogo. */
    @PostMapping
    public ResponseEntity<Libro> crear(@RequestBody CrearLibroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crearLibro(request));
    }
}