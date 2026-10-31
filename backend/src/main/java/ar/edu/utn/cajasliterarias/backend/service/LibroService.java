package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;

    /** Catalogo de libros para armar la curaduria de una edicion, ordenado por titulo. */
    public List<Libro> listarLibros() {
        return libroRepository.findAllByOrderByTituloAsc();
    }
}