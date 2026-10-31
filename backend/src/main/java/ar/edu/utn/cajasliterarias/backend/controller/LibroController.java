package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.service.LibroService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}