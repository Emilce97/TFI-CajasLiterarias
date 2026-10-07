package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EdicionRepository extends JpaRepository<Edicion, Long> {

    // Para encontrar la edicion actualmente abierta al momento del corte
    Edicion findByEstado(EstadoEdicion estado);

    // Para evitar crear dos ediciones con el mismo nombre/mes por error
    boolean existsByNombre(String nombre);

    // Para el cierre: lee la edicion con bloqueo de escritura (SELECT ... FOR UPDATE).
    // Si dos solicitudes de cierre llegan a la vez, la segunda espera a que termine la primera
    // y, cuando obtiene la fila, está ya CERRADA.
    @Query(value = "SELECT * FROM edicion WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Edicion> findByIdForUpdate(@Param("id") Long id);
}
