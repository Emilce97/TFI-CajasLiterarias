package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    // Para validar el cupoMaximo de una categoria al momento del alta/cambio
    long countByCategoriaIdAndEstado(Long categoriaId, EstadoSuscripcion estado);

    // Para evitar que un mismo suscriptor tenga dos altas activas en la misma categoria.
    boolean existsBySuscriptorIdAndCategoriaIdAndEstado(Long suscriptorId, Long categoriaId, EstadoSuscripcion estado
    );

    // Candidatas al corte: TODAS las suscripciones activas, tengan o no pago cargado para la edición
    // (el filtro por pago validado se hace dentro del loop del CierreEdicionService, no acá).
    List<Suscripcion> findByEstado(EstadoSuscripcion estado);

}
