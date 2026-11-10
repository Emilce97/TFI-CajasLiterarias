package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findAllByOrderByNombreAsc();

    // IgnoreCase: "Editorial X" y "editorial x" cuentan como el mismo proveedor,
    // tambien en TiDB (que por defecto distingue mayusculas).
    boolean existsByNombreIgnoreCase(String nombre);
}