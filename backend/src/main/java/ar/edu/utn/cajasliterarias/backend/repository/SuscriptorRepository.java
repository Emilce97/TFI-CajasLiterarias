package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuscriptorRepository extends JpaRepository<Suscriptor, Long> {

    Optional<Suscriptor> findByEmail(String email);
}