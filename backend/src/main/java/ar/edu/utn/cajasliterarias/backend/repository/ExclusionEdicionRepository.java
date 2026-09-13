package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.ExclusionEdicion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExclusionEdicionRepository extends JpaRepository<ExclusionEdicion, Long> {

    // Guardado de idempotencia adicional a PedidoEdicionRepository.existsByEdicionId.
    // La validación principal sigue siendo la de PedidoEdicion; esta evita reprocesar
    // exclusiones ya registradas, incluso si no se generaron pedidos.
    boolean existsByEdicionId(Long edicionId);
}
