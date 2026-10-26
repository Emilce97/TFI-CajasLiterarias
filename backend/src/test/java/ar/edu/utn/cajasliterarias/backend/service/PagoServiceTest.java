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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import ar.edu.utn.cajasliterarias.backend.repository.AdministradoraRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private SuscripcionRepository suscripcionRepository;
    @Mock
    private EdicionRepository edicionRepository;
    @Mock
    private AdministradoraRepository administradoraRepository;

    private static final String EMAIL_ADMIN = "admin@cajasliterarias.com";

    private PagoService service;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));

    @BeforeEach
    void setUp() {
        service = new PagoService(pagoRepository, suscripcionRepository, edicionRepository,
                administradoraRepository, clock);
    }

    // helpers

    private Edicion edicion(EstadoEdicion estado) {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        edicion.setEstado(estado);
        return edicion;
    }

    private Suscripcion suscripcion(EstadoSuscripcion estado) {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(10L);
        suscripcion.setEstado(estado);
        return suscripcion;
    }

    private CrearPagoRequest request(BigDecimal monto) {
        CrearPagoRequest request = new CrearPagoRequest();
        request.setSuscripcionId(10L);
        request.setEdicionId(1L);
        request.setMonto(monto);
        return request;
    }

    private Pago pago(EstadoPago estado, Edicion edicion) {
        Pago pago = new Pago();
        pago.setId(5L);
        pago.setEdicion(edicion);
        pago.setEstado(estado);
        return pago;
    }

    private Administradora admin() {
        Administradora admin = new Administradora();
        admin.setId(1L);
        admin.setEmail(EMAIL_ADMIN);
        return admin;
    }

    // registrarPago
    @Test
    void registrarPago_valido_guardaPendienteConFechaDelReloj() {
        when(suscripcionRepository.findById(10L)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA)));
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.ABIERTA)));
        when(pagoRepository.existsBySuscripcionIdAndEdicionId(10L, 1L)).thenReturn(false);
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago resultado = service.registrarPago(request(new BigDecimal("5000.00")));

        assertEquals(EstadoPago.PENDIENTE, resultado.getEstado());
        assertEquals(new BigDecimal("5000.00"), resultado.getMonto());
        assertEquals(LocalDate.of(2026, 10, 10), resultado.getFechaPago());
        assertNull(resultado.getFechaValidacion());
    }

    @Test
    void registrarPago_conMontoNull_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarPago(request(null)));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-5000.50"})
    void registrarPago_conMontoCeroONegativo_lanzaIllegalArgument(String monto) {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarPago(request(new BigDecimal(monto))));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPago_paraEdicionCerrada_lanzaIllegalState() {
        when(suscripcionRepository.findById(10L)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA)));
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));

        assertThrows(IllegalStateException.class,
                () -> service.registrarPago(request(new BigDecimal("5000"))));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoSuscripcion.class, names = {"PAUSADA", "BAJA"})
    void registrarPago_paraSuscripcionNoActiva_lanzaIllegalState(EstadoSuscripcion estado) {
        when(suscripcionRepository.findById(10L)).thenReturn(Optional.of(suscripcion(estado)));
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.ABIERTA)));

        assertThrows(IllegalStateException.class,
                () -> service.registrarPago(request(new BigDecimal("5000"))));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPago_duplicado_lanzaIllegalStateYNoGuarda() {
        when(suscripcionRepository.findById(10L)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA)));
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.ABIERTA)));
        when(pagoRepository.existsBySuscripcionIdAndEdicionId(10L, 1L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.registrarPago(request(new BigDecimal("5000"))));

        assertTrue(ex.getMessage().contains("Ya existe un pago"));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPago_dosSolicitudesSimultaneas_elUniqueDeLaBaseSeTraduceAErrorControlado() {
        // Las dos pasaron el chequeo previo; la base rechaza a la segunda.
        when(suscripcionRepository.findById(10L)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA)));
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.ABIERTA)));
        when(pagoRepository.existsBySuscripcionIdAndEdicionId(10L, 1L)).thenReturn(false);
        when(pagoRepository.saveAndFlush(any(Pago.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThrows(IllegalStateException.class,
                () -> service.registrarPago(request(new BigDecimal("5000"))));
    }

    @Test
    void registrarPago_conSuscripcionInexistente_lanzaIllegalArgument() {
        when(suscripcionRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrarPago(request(new BigDecimal("5000"))));

        assertTrue(ex.getMessage().contains("No existe la suscripcion"));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarPago_conEdicionInexistente_lanzaIllegalArgument() {
        when(suscripcionRepository.findById(10L)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA)));
        when(edicionRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrarPago(request(new BigDecimal("5000"))));

        assertTrue(ex.getMessage().contains("No existe la edicion"));
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    // validarPago
    @Test
    void validarPago_pendiente_quedaValidadoConFechaHoraYAdministradora() {
        Pago pendiente = pago(EstadoPago.PENDIENTE, edicion(EstadoEdicion.ABIERTA));
        when(pagoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(pendiente));
        when(administradoraRepository.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.of(admin()));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago resultado = service.validarPago(5L, EMAIL_ADMIN);

        assertEquals(EstadoPago.VALIDADO, resultado.getEstado());
        assertEquals(LocalDateTime.of(2026, 10, 10, 12, 0), resultado.getFechaValidacion());
        assertEquals(EMAIL_ADMIN, resultado.getValidadoPor().getEmail());
    }

    @Test
    void validarPago_dobleValidacion_lanzaIllegalStateYNoPisaLaFecha() {
        LocalDateTime primeraValidacion = LocalDateTime.of(2026, 10, 1, 9, 30);
        Pago yaValidado = pago(EstadoPago.VALIDADO, edicion(EstadoEdicion.ABIERTA));
        yaValidado.setFechaValidacion(primeraValidacion);
        when(pagoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(yaValidado));

        assertThrows(IllegalStateException.class, () -> service.validarPago(5L, EMAIL_ADMIN));

        // La prioridad por cupo depende de esta fecha: no se puede modificar.
        assertEquals(primeraValidacion, yaValidado.getFechaValidacion());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void validarPago_deEdicionCerrada_lanzaIllegalState() {
        Pago pendiente = pago(EstadoPago.PENDIENTE, edicion(EstadoEdicion.CERRADA));
        when(pagoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(pendiente));

        assertThrows(IllegalStateException.class, () -> service.validarPago(5L, EMAIL_ADMIN));
        assertEquals(EstadoPago.PENDIENTE, pendiente.getEstado());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void validarPago_inexistente_lanzaIllegalArgument() {
        when(pagoRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.validarPago(99L, EMAIL_ADMIN));
    }

    @Test
    void validarPago_conAdministradoraInexistente_lanzaIllegalStateYNoGuarda() {
        Pago pendiente = pago(EstadoPago.PENDIENTE, edicion(EstadoEdicion.ABIERTA));
        when(pagoRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(pendiente));
        when(administradoraRepository.findByEmail(EMAIL_ADMIN)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.validarPago(5L, EMAIL_ADMIN));
        assertEquals(EstadoPago.PENDIENTE, pendiente.getEstado());
        verify(pagoRepository, never()).save(any());
    }

    // listarPendientes

    @Test
    void listarPendientes_devuelveSoloLosPendientes() {
        Pago pendiente = pago(EstadoPago.PENDIENTE, edicion(EstadoEdicion.ABIERTA));
        when(pagoRepository.findByEstado(EstadoPago.PENDIENTE)).thenReturn(List.of(pendiente));

        assertEquals(List.of(pendiente), service.listarPendientes());
    }

    @Test
    void listarValidados_devuelveLosValidadosDelMasRecienteAlMasViejo() {
        Pago validado = pago(EstadoPago.VALIDADO, edicion(EstadoEdicion.ABIERTA));
        when(pagoRepository.findByEstadoOrderByFechaValidacionDesc(EstadoPago.VALIDADO))
                .thenReturn(List.of(validado));

        assertEquals(List.of(validado), service.listarValidados());
    }
}
