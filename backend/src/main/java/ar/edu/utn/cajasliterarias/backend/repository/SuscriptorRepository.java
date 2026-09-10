package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuscriptorRepository extends JpaRepository<Suscriptor, Long> {
}