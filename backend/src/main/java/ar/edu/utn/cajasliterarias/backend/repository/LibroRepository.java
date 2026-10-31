package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {
    // Catalogo para el formulario de nueva edicion, ordenado alfabeticamente.
    List<Libro> findAllByOrderByTituloAsc();
}
