package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearLibroRequest;
import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import ar.edu.utn.cajasliterarias.backend.repository.LibroRepository;
import ar.edu.utn.cajasliterarias.backend.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catalogo de libros. Datos mal cargados, titulo repetido o proveedor inexistente
 * -> IllegalArgumentException (400).
 */
@Service
@RequiredArgsConstructor
public class LibroService {

    private static final int LARGO_MAXIMO_TITULO = 255; // igual que la columna libro.titulo
    private static final int LARGO_MAXIMO_AUTOR = 150;  // igual que la columna libro.autor

    private final LibroRepository libroRepository;
    private final ProveedorRepository proveedorRepository;

    /** Catalogo de libros para armar la curaduria de una edicion, ordenado por titulo. */
    public List<Libro> listarLibros() {
        return libroRepository.findAllByOrderByTituloAsc();
    }

    @Transactional
    public Libro crearLibro(CrearLibroRequest request) {
        if (request == null || request.titulo() == null || request.titulo().isBlank()) {
            throw new IllegalArgumentException("El título del libro es obligatorio.");
        }
        String titulo = request.titulo().trim();
        if (titulo.length() > LARGO_MAXIMO_TITULO) {
            throw new IllegalArgumentException(
                    "El título no puede superar los " + LARGO_MAXIMO_TITULO + " caracteres.");
        }

        // El autor es opcional: si viene vacio se guarda como null.
        String autor = request.autor() == null || request.autor().isBlank()
                ? null
                : request.autor().trim();
        if (autor != null && autor.length() > LARGO_MAXIMO_AUTOR) {
            throw new IllegalArgumentException(
                    "El autor no puede superar los " + LARGO_MAXIMO_AUTOR + " caracteres.");
        }

        // El proveedor es opcional, pero si viene tiene que existir.
        Proveedor proveedor = null;
        if (request.proveedorId() != null) {
            proveedor = proveedorRepository.findById(request.proveedorId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe el proveedor con id " + request.proveedorId() + "."));
        }

        if (libroRepository.existsByTituloIgnoreCase(titulo)) {
            throw new IllegalArgumentException("Ya existe un libro con el título '" + titulo + "'.");
        }

        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setProveedor(proveedor);
        return libroRepository.save(libro);
    }
}