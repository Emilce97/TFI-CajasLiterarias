package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Pago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Para el service del corte: traer solo los pagos validados de una edicion
    List<Pago> findByEdicionIdAndEstado(Long edicionId, EstadoPago estado);

    // Para el loop del corte: TODOS los pagos de la edición (cualquier estado) en una sola query.
    // Se usa para armar un mapa suscripcionId -> Pago en memoria y evitar hacer una query por suscripción dentro del loop (N+1).
    // Una suscripción sin pago cargado para esta edición simplemente no aparece en el resultado.
    List<Pago> findByEdicionId(Long edicionId);

    // Para listar los pagos pendientes de validar (panel de conciliacion)
    List<Pago> findByEstado(EstadoPago estado);

    // Chequeo previo para dar un mensaje claro. La garantia real es la restriccion UNIQUE
    // uk_pago_suscripcion_edicion (suscripcion_id, edicion_id) en la base de datos.
    boolean existsBySuscripcionIdAndEdicionId(Long suscripcionId, Long edicionId);

    // Para validar: bloquea la fila (SELECT ... FOR UPDATE) para que dos validaciones simultaneas
    // del mismo pago se serialicen y la segunda vea el estado ya VALIDADO.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> findByIdForUpdate(@Param("id") Long id);
}