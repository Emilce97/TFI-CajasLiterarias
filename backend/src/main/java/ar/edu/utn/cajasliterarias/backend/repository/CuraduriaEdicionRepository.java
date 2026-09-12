package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.CuraduriaEdicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuraduriaEdicionRepository extends JpaRepository<CuraduriaEdicion, Long> {

    // Para saber que libro corresponde a cada categoria en una edicion dada
    List<CuraduriaEdicion> findByEdicionId(Long edicionId);

    // Para obtener el cupoMaximo de una categoria puntual en la edicion abierta
    // al momento de validar un alta/cambio de suscripcion.
    Optional<CuraduriaEdicion> findByEdicionIdAndCategoriaId(Long edicionId, Long categoriaId);

}