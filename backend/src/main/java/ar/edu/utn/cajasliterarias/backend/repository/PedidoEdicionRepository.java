package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoEdicionRepository extends JpaRepository<PedidoEdicion, Long> {
}