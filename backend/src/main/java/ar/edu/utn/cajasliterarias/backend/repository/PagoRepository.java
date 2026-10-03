package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.Pago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Para el service del corte: traer solo los pagos validados de una edicion
    List<Pago> findByEdicionIdAndEstado(Long edicionId, EstadoPago estado);

    // Para el loop del corte: TODOS los pagos de la edición (cualquier estado) en una sola query.
    // Se usa para armar un mapa suscripcionId -> Pago en memoria y evitar hacer una query por suscripción dentro del loop (N+1).
    // Una suscripción sin pago cargado para esta edición simplemente no aparece en el resultado.
    List<Pago> findByEdicionId(Long edicionId);

    // Para listar los pagos pendientes de validar (panel de conciliacion)
    List<Pago> findByEstado(EstadoPago estado);

    // Chequeo defensivo: evitar registrar dos pagos para la misma suscripcion+edicion.
// Nota: esto es solo un chequeo a nivel aplicacion. Falta agregar la restriccion
// UNIQUE real en la base de datos (pendiente, tarea de Nati segun devolucion de Oscar).
    boolean existsBySuscripcionIdAndEdicionId(Long suscripcionId, Long edicionId);
}