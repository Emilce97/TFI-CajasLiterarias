package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearPagoRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.Pago;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.PagoRepository;
import ar.edu.utn.cajasliterarias.backend.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final EdicionRepository edicionRepository;

    public PagoService(
            PagoRepository pagoRepository,
            SuscripcionRepository suscripcionRepository,
            EdicionRepository edicionRepository
    ) {
        this.pagoRepository = pagoRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.edicionRepository = edicionRepository;
    }

    /**
     * Registra un pago nuevo, en estado PENDIENTE hasta que alguien lo valide.
     */
    @Transactional
    public Pago registrarPago(CrearPagoRequest request) {
        Suscripcion suscripcion = suscripcionRepository.findById(request.getSuscripcionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la suscripcion con id " + request.getSuscripcionId()
                ));

        Edicion edicion = edicionRepository.findById(request.getEdicionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la edicion con id " + request.getEdicionId()
                ));

        if (pagoRepository.existsBySuscripcionIdAndEdicionId(
                request.getSuscripcionId(), request.getEdicionId())) {
            throw new IllegalArgumentException(
                    "Ya existe un pago registrado para esta suscripcion en esta edicion."
            );
        }

        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEdicion(edicion);
        pago.setMonto(request.getMonto());
        pago.setFechaPago(LocalDate.now());
        pago.setEstado(EstadoPago.PENDIENTE);

        return pagoRepository.save(pago);
    }

    /**
     * Lista los pagos pendientes de validar (para el panel de conciliacion).
     */
    public List<Pago> listarPendientes() {
        return pagoRepository.findByEstado(EstadoPago.PENDIENTE);
    }

    /**
     * Marca un pago como validado.
     */
    @Transactional
    public Pago validarPago(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el pago con id " + pagoId
                ));

        pago.setEstado(EstadoPago.VALIDADO);
        pago.setFechaValidacion(LocalDate.now());

        return pagoRepository.save(pago);
    }
}