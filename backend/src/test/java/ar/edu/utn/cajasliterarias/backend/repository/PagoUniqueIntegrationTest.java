package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contra la base real (MySQL local): comprueba que las restricciones de la tabla pago
 * existen de verdad, no solo en el service.
 */
@SpringBootTest
@Transactional
class PagoUniqueIntegrationTest {

    @Autowired private PagoRepository pagoRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private SuscriptorRepository suscriptorRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private EdicionRepository edicionRepository;

    private Suscripcion suscripcion;
    private Edicion edicion;

    @BeforeEach
    void setUp() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Pago-IT-" + UUID.randomUUID().toString().substring(0, 8));
        categoriaRepository.save(categoria);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setNombre("Suscriptora Pago IT");
        suscriptor.setEmail("pago-it-" + UUID.randomUUID() + "@test.com");
        suscriptor.setDireccion("Calle Falsa 123");
        suscriptor.setFechaRegistro(LocalDate.now());
        suscriptorRepository.save(suscriptor);

        suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaAlta(LocalDate.now());
        suscripcionRepository.save(suscripcion);

        edicion = new Edicion();
        edicion.setNombre("Edicion Pago IT " + UUID.randomUUID().toString().substring(0, 8));
        edicion.setFechaCorte(LocalDate.now().plusDays(5));
        edicion.setEstado(EstadoEdicion.CERRADA); // CERRADA para no chocar con una ABIERTA de pruebas manuales
        edicionRepository.save(edicion);
    }

    private Pago nuevoPago(BigDecimal monto) {
        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEdicion(edicion);
        pago.setMonto(monto);
        pago.setFechaPago(LocalDate.now());
        pago.setEstado(EstadoPago.PENDIENTE);
        return pago;
    }

    @Test
    void dosPagosParaLaMismaSuscripcionYEdicion_laBaseRechazaElSegundo() {
        pagoRepository.saveAndFlush(nuevoPago(new BigDecimal("5000.00")));

        assertThatThrownBy(() -> pagoRepository.saveAndFlush(nuevoPago(new BigDecimal("5000.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void fechaValidacionConservaLaHora() {
        Pago pago = nuevoPago(new BigDecimal("5000.00"));
        pago.setEstado(EstadoPago.VALIDADO);
        LocalDateTime validacion = LocalDateTime.of(2026, 10, 10, 18, 30, 15);
        pago.setFechaValidacion(validacion);
        pagoRepository.saveAndFlush(pago);

        Pago leido = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(leido.getFechaValidacion()).isEqualTo(validacion);
    }
}
