package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearPagoRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.Pago;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.PagoRepository;
import ar.edu.utn.cajasliterarias.backend.repository.SuscripcionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final EdicionRepository edicionRepository;
    private final Clock clock;

    public PagoService(
            PagoRepository pagoRepository,
            SuscripcionRepository suscripcionRepository,
            EdicionRepository edicionRepository,
            Clock clock
    ) {
        this.pagoRepository = pagoRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.edicionRepository = edicionRepository;
        this.clock = clock;
    }

    /**
     * Registra un pago nuevo, en estado PENDIENTE hasta que alguien lo valide.
     * Reglas (P0):
     * - el monto es obligatorio y mayor a cero;
     * - la edicion debe estar ABIERTA y la suscripcion ACTIVA;
     * - como maximo un pago por suscripcion y edicion (chequeo previo + UNIQUE en la base).
     * Datos mal formados -> IllegalArgumentException (400).
     * Conflictos de estado (edicion cerrada, suscripcion no activa, pago duplicado) -> IllegalStateException (409).
     */
    @Transactional
    public Pago registrarPago(CrearPagoRequest request) {
        if (request == null || request.getSuscripcionId() == null || request.getEdicionId() == null) {
            throw new IllegalArgumentException("Se requieren suscripcionId y edicionId.");
        }
        BigDecimal monto = request.getMonto();
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago es obligatorio y debe ser mayor a cero.");
        }

        Suscripcion suscripcion = suscripcionRepository.findById(request.getSuscripcionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la suscripcion con id " + request.getSuscripcionId()
                ));

        Edicion edicion = edicionRepository.findById(request.getEdicionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la edicion con id " + request.getEdicionId()
                ));

        if (edicion.getEstado() != EstadoEdicion.ABIERTA) {
            throw new IllegalStateException(
                    "No se pueden registrar pagos para la edicion " + edicion.getId() + ": no esta abierta."
            );
        }

        if (suscripcion.getEstado() != EstadoSuscripcion.ACTIVA) {
            throw new IllegalStateException(
                    "No se puede registrar un pago para una suscripcion en estado " + suscripcion.getEstado() + "."
            );
        }

        if (pagoRepository.existsBySuscripcionIdAndEdicionId(suscripcion.getId(), edicion.getId())) {
            throw pagoDuplicado();

        }

        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEdicion(edicion);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDate.now(clock));
        pago.setEstado(EstadoPago.PENDIENTE);

        try {
            // saveAndFlush: si dos solicitudes pasaron el chequeo previo a la vez,
            // el UNIQUE de la base rechaza a la segunda y lo traducimos a un error controlado.
            return pagoRepository.saveAndFlush(pago);
        } catch (DataIntegrityViolationException e) {
            throw pagoDuplicado();
        }
    }

    /**
     * Lista los pagos pendientes de validar (para el panel de conciliacion).
     */
    public List<Pago> listarPendientes() {
        return pagoRepository.findByEstado(EstadoPago.PENDIENTE);
    }

    /**
     * Marca un pago como validado, registrando fecha y hora.
     * Un pago se valida una sola vez: la fecha/hora de validacion define la prioridad por cupo en el corte,
     * asi que no puede pisarse. La fila se lee con bloqueo para que dos validaciones simultaneas
     * se serialicen. Tampoco se valida un pago de una edicion que ya no esta abierta.
     */
    @Transactional
    public Pago validarPago(Long pagoId) {
        Pago pago = pagoRepository.findByIdForUpdate(pagoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el pago con id " + pagoId
                ));

        if (pago.getEstado() == EstadoPago.VALIDADO) {
            throw new IllegalStateException(
                    "El pago " + pagoId + " ya fue validado el " + pago.getFechaValidacion() + "."
            );
        }

        if (pago.getEdicion().getEstado() != EstadoEdicion.ABIERTA) {
            throw new IllegalStateException(
                    "No se puede validar el pago " + pagoId + ": la edicion " + pago.getEdicion().getId()
                            + " no esta abierta."
            );
        }

        pago.setEstado(EstadoPago.VALIDADO);
        pago.setFechaValidacion(LocalDateTime.now(clock));

        return pagoRepository.save(pago);
    }

    private IllegalStateException pagoDuplicado() {
        return new IllegalStateException(
                "Ya existe un pago registrado para esta suscripcion en esta edicion."
        );
    }

}