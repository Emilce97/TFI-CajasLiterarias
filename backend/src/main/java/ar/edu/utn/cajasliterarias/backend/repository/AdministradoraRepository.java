package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradoraRepository extends JpaRepository<Administradora, Long> {
    Optional<Administradora> findByEmail(String email);
}
