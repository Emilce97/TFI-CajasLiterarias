package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdicionRepository extends JpaRepository<Edicion, Long> {

    // Para encontrar la edicion actualmente abierta al momento del corte
    Edicion findByEstado(EstadoEdicion estado);
}
