package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroRepository extends JpaRepository<Libro, Long> {
}
