package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.CuraduriaEdicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuraduriaEdicionRepository extends JpaRepository<CuraduriaEdicion, Long> {

    // Para saber que libro corresponde a cada categoria en una edicion dada
    List<CuraduriaEdicion> findByEdicionId(Long edicionId);
}