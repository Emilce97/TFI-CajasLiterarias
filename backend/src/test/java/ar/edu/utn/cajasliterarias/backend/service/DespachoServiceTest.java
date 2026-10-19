package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarEstadoPedidoRequest;
import ar.edu.utn.cajasliterarias.backend.dto.PedidoDespachoResponse;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;
import ar.edu.utn.cajasliterarias.backend.repository.AdministradoraRepository;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.PedidoEdicionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
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
class DespachoServiceTest {

    private static final String EMAIL = "admin@cajasliterarias.com";

    @Mock private PedidoEdicionRepository pedidoRepository;
    @Mock private EdicionRepository edicionRepository;
    @Mock private AdministradoraRepository administradoraRepository;

    private DespachoService service;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));

    @BeforeEach
    void setUp() {
        service = new DespachoService(pedidoRepository, edicionRepository, administradoraRepository, clock);
    }

    // helpers

    private Edicion edicion(EstadoEdicion estado) {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        edicion.setEstado(estado);
        return edicion;
    }

    private PedidoEdicion pedido(EstadoPedido estado) {
        PedidoEdicion pedido = new PedidoEdicion();
        pedido.setId(7L);
        pedido.setEstadoPedido(estado);
        pedido.setNombreDestinatario("Ana Pérez");
        pedido.setDireccionEntrega("Calle Falsa 123");
        pedido.setCategoriaCongelada("Romance");
        pedido.setLibroCongelado("Orgullo y prejuicio");
        return pedido;
    }

    private Administradora admin() {
        Administradora admin = new Administradora();
        admin.setId(1L);
        admin.setEmail(EMAIL);
        return admin;
    }

    private CambiarEstadoPedidoRequest request(EstadoPedido estado, String tracking, String observaciones) {
        return new CambiarEstadoPedidoRequest(estado, tracking, observaciones);
    }

    // listarPedidos

    @Test
    void listarPedidos_edicionCerradaSinEstado_devuelveTodosComoDto() {
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));
        when(pedidoRepository.findByEdicionIdOrderByIdAsc(1L))
                .thenReturn(List.of(pedido(EstadoPedido.PENDIENTE_DE_EMPAQUE)));

        List<PedidoDespachoResponse> resultado = service.listarPedidos(1L, null);

        assertEquals(1, resultado.size());
        assertEquals("Ana Pérez", resultado.get(0).nombreDestinatario());
        assertEquals(EstadoPedido.PENDIENTE_DE_EMPAQUE, resultado.get(0).estado());
        verify(pedidoRepository, never()).findByEdicionIdAndEstadoPedidoOrderByIdAsc(any(), any());
    }

    @Test
    void listarPedidos_conEstado_usaLaBusquedaFiltrada() {
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));
        when(pedidoRepository.findByEdicionIdAndEstadoPedidoOrderByIdAsc(1L, EstadoPedido.EMPAQUETADO))
                .thenReturn(List.of(pedido(EstadoPedido.EMPAQUETADO)));

        List<PedidoDespachoResponse> resultado = service.listarPedidos(1L, EstadoPedido.EMPAQUETADO);

        assertEquals(EstadoPedido.EMPAQUETADO, resultado.get(0).estado());
        verify(pedidoRepository, never()).findByEdicionIdOrderByIdAsc(any());
    }

    @Test
    void listarPedidos_edicionAbierta_lanzaIllegalState() {
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.ABIERTA)));

        assertThrows(IllegalStateException.class, () -> service.listarPedidos(1L, null));
        verify(pedidoRepository, never()).findByEdicionIdOrderByIdAsc(any());
    }

    @Test
    void listarPedidos_edicionInexistente_lanzaIllegalArgument() {
        when(edicionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.listarPedidos(99L, null));
    }

    // cambiarEstado

    @Test
    void cambiarEstado_pendienteAEmpaquetado_avanzaYRegistraQuienYCuando() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido(EstadoPedido.PENDIENTE_DE_EMPAQUE)));
        when(administradoraRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin()));
        when(pedidoRepository.save(any(PedidoEdicion.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDespachoResponse resultado = service.cambiarEstado(
                7L, request(EstadoPedido.EMPAQUETADO, null, "Caja armada"), EMAIL);

        assertEquals(EstadoPedido.EMPAQUETADO, resultado.estado());
        assertEquals("Caja armada", resultado.observaciones());
        assertEquals(EMAIL, resultado.modificadoPor());
        // 15:00 UTC = 12:00 en Buenos Aires, con hora (por eso es LocalDateTime).
        assertEquals(LocalDateTime.of(2026, 10, 10, 12, 0), resultado.fechaModificacion());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPedido.class, names = {"PENDIENTE_DE_EMPAQUE", "EMPAQUETADO", "ENTREGADO"})
    void cambiarEstado_desdeEmpaquetadoANoSiguiente_lanzaIllegalStateYNoGuarda(EstadoPedido destino) {
        // Retroceder, repetir el mismo estado o saltear un paso: solo vale DESPACHADO.
        PedidoEdicion empaquetado = pedido(EstadoPedido.EMPAQUETADO);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(empaquetado));

        assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(7L, request(destino, "AR123456", null), EMAIL));

        assertEquals(EstadoPedido.EMPAQUETADO, empaquetado.getEstadoPedido());
        verify(pedidoRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void cambiarEstado_despacharSinTracking_lanzaIllegalArgumentYNoGuarda(String tracking) {
        PedidoEdicion empaquetado = pedido(EstadoPedido.EMPAQUETADO);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(empaquetado));

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(7L, request(EstadoPedido.DESPACHADO, tracking, null), EMAIL));

        assertEquals(EstadoPedido.EMPAQUETADO, empaquetado.getEstadoPedido());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_despacharConTracking_guardaTrackingYConservaObservaciones() {
        PedidoEdicion empaquetado = pedido(EstadoPedido.EMPAQUETADO);
        empaquetado.setObservaciones("Caja armada");
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(empaquetado));
        when(administradoraRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin()));
        when(pedidoRepository.save(any(PedidoEdicion.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoDespachoResponse resultado = service.cambiarEstado(
                7L, request(EstadoPedido.DESPACHADO, "  AR123456  ", null), EMAIL);

        assertEquals(EstadoPedido.DESPACHADO, resultado.estado());
        assertEquals("AR123456", resultado.tracking());
        assertEquals("Caja armada", resultado.observaciones());
    }

    @Test
    void cambiarEstado_pedidoEntregado_lanzaIllegalState() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido(EstadoPedido.ENTREGADO)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(7L, request(EstadoPedido.ENTREGADO, null, null), EMAIL));

        assertTrue(ex.getMessage().contains("entregado"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_sinEstado_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(7L, request(null, null, null), EMAIL));
        verify(pedidoRepository, never()).findById(any());
    }

    @Test
    void cambiarEstado_pedidoInexistente_lanzaIllegalArgument() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(99L, request(EstadoPedido.EMPAQUETADO, null, null), EMAIL));
    }
}