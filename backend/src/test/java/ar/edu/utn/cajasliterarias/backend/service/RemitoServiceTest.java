package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.RemitoResponse;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.repository.PedidoEdicionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemitoServiceTest {

    @Mock private PedidoEdicionRepository pedidoEdicionRepository;

    private RemitoService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-17T15:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires"));
        service = new RemitoService(pedidoEdicionRepository, clock);
    }

    // helper: el suscriptor "vivo" ya cambió sus datos, pero el snapshot conserva los del corte
    private PedidoEdicion pedido(EstadoPedido estado) {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        edicion.setNombre("Edicion Octubre");

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setNombre("Nombre Nuevo");
        suscriptor.setDireccion("Direccion Nueva 999");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);

        PedidoEdicion pedido = new PedidoEdicion();
        pedido.setId(43L);
        pedido.setEdicion(edicion);
        pedido.setSuscripcion(suscripcion);
        pedido.setNombreDestinatario("Nombre Original");
        pedido.setDireccionEntrega("Calle Falsa 123");
        pedido.setCategoriaCongelada("Romance");
        pedido.setLibroCongelado("Libro de prueba - Romance");
        pedido.setEstadoPedido(estado);
        return pedido;
    }

    @Test
    void generarRemito_pedidoEmpaquetado_usaLosDatosCongeladosDelSnapshot() {
        when(pedidoEdicionRepository.findById(43L)).thenReturn(Optional.of(pedido(EstadoPedido.EMPAQUETADO)));

        RemitoResponse remito = service.generarRemito(43L);

        assertEquals(43L, remito.getNumero());
        assertEquals(LocalDate.of(2026, 10, 17), remito.getFechaEmision());
        assertEquals("Edicion Octubre", remito.getEdicion());
        assertEquals("Nombre Original", remito.getNombreDestinatario());
        assertEquals("Calle Falsa 123", remito.getDireccionEntrega());
        assertEquals("Romance", remito.getCategoria());
        assertEquals("Libro de prueba - Romance", remito.getLibro());
        assertEquals(EstadoPedido.EMPAQUETADO, remito.getEstado());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPedido.class, names = {"EMPAQUETADO", "DESPACHADO", "ENTREGADO"})
    void generarRemito_desdeEmpaquetadoEnAdelante_emite(EstadoPedido estado) {
        when(pedidoEdicionRepository.findById(43L)).thenReturn(Optional.of(pedido(estado)));

        assertEquals(estado, service.generarRemito(43L).getEstado());
    }

    @Test
    void generarRemito_pedidoPendienteDeEmpaque_lanzaIllegalState() {
        when(pedidoEdicionRepository.findById(43L)).thenReturn(Optional.of(pedido(EstadoPedido.PENDIENTE_DE_EMPAQUE)));

        assertThrows(IllegalStateException.class, () -> service.generarRemito(43L));
    }

    @Test
    void generarRemito_pedidoInexistente_lanzaIllegalArgument() {
        when(pedidoEdicionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.generarRemito(99L));
    }
}
