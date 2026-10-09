package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.DemandaEdicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandaEdicionRepository extends JpaRepository<DemandaEdicion, Long> {

    // Para el panel de demanda: todas las filas (una por libro) de una edicion.
    List<DemandaEdicion> findByEdicionId(Long edicionId);
}