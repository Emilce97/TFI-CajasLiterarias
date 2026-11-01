package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarEstadoPedidoRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CrearPagoRequest;
import ar.edu.utn.cajasliterarias.backend.dto.PedidoDespachoResponse;
import ar.edu.utn.cajasliterarias.backend.dto.RemitoResponse;
import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba contra la base real el circuito completo de la administradora:
 * pago -> validacion -> corte -> pedidos -> despacho -> remito.
 * Usa los services reales (no mocks). Todo se revierte al terminar cada test (@Transactional).
 */
@SpringBootTest
@Transactional
class CircuitoAdminIntegrationTest {

    @Autowired private PagoService pagoService;
    @Autowired private CierreEdicionService cierreEdicionService;
    @Autowired private DespachoService despachoService;
    @Autowired private RemitoService remitoService;

    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private EdicionRepository edicionRepository;
    @Autowired private CuraduriaEdicionRepository curaduriaEdicionRepository;
    @Autowired private SuscriptorRepository suscriptorRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private AdministradoraRepository administradoraRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PedidoEdicionRepository pedidoEdicionRepository;
    @Autowired private Clock clock;

    private Categoria categoria;
    private Libro libro;
    private Suscriptor suscriptor;
    private Suscripcion suscripcion;
    private Administradora admin;
    private Edicion edicion;

    @BeforeEach
    void setUp() {
        // La base local puede tener una edicion ABIERTA de pruebas manuales:
        // se cierra dentro de esta transaccion (se revierte al terminar el test).
        Edicion previa = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        if (previa != null) {
            previa.setEstado(EstadoEdicion.CERRADA);
            edicionRepository.save(previa);
        }

        categoria = new Categoria();
        categoria.setNombre("Categoria CIR " + UUID.randomUUID().toString().substring(0, 8));
        categoriaRepository.save(categoria);

        libro = new Libro();
        libro.setTitulo("Libro CIR " + UUID.randomUUID().toString().substring(0, 8));
        libroRepository.save(libro);

        admin = new Administradora();
        admin.setNombre("Admin CIR");
        admin.setEmail("admin-cir-" + UUID.randomUUID() + "@test.com");
        admin.setPasswordHash("no-se-usa-en-este-test");
        administradoraRepository.save(admin);

        suscriptor = new Suscriptor();
        suscriptor.setNombre("Suscriptora CIR");
        suscriptor.setEmail("cir-" + UUID.randomUUID() + "@test.com");
        suscriptor.setDireccion("Calle Falsa 123");
        suscriptor.setFechaRegistro(LocalDate.now(clock));
        suscriptorRepository.save(suscriptor);

        suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaAlta(LocalDate.now(clock));
        suscripcionRepository.save(suscripcion);

        edicion = new Edicion();
        edicion.setNombre("Edicion CIR " + UUID.randomUUID().toString().substring(0, 8));
        edicion.setFechaCorte(LocalDate.now(clock).plusDays(5));
        edicion.setEstado(EstadoEdicion.ABIERTA);
        edicionRepository.save(edicion);

        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setEdicion(edicion);
        curaduria.setCategoria(categoria);
        curaduria.setLibro(libro);
        curaduria.setPrecioVigente(new BigDecimal("5000.00"));
        curaduria.setCupoMaximo(10);
        curaduriaEdicionRepository.save(curaduria);
    }

    @Test
    void circuitoCompleto_delPagoAlRemito() {
        // 1. La suscriptora registra el pago: queda pendiente.
        Pago pago = pagoService.registrarPago(pedidoDePago());
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);

        // 2. La admin lo valida: queda registrado quien y cuando.
        Pago validado = pagoService.validarPago(pago.getId(), admin.getEmail());
        assertThat(validado.getEstado()).isEqualTo(EstadoPago.VALIDADO);
        assertThat(validado.getFechaValidacion()).isNotNull();
        assertThat(validado.getValidadoPor().getEmail()).isEqualTo(admin.getEmail());

        // 3. Corte: se genera el pedido de esta suscripcion y la edicion queda cerrada.
        ResumenCierreDTO resumen = cierreEdicionService.ejecutarCorte(edicion.getId());
        assertThat(resumen.getTotalPedidosGenerados()).isEqualTo(1);
        assertThat(resumen.getSuscripcionesExcluidas()).doesNotContain(suscripcion.getId());
        assertThat(edicionRepository.findById(edicion.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoEdicion.CERRADA);

        // 4. Panel de despacho: el pedido aparece con los datos congelados en el corte.
        List<PedidoDespachoResponse> pedidos = despachoService.listarPedidos(edicion.getId(), null);
        assertThat(pedidos).hasSize(1);
        PedidoDespachoResponse pedido = pedidos.get(0);
        assertThat(pedido.nombreDestinatario()).isEqualTo("Suscriptora CIR");
        assertThat(pedido.direccionEntrega()).isEqualTo("Calle Falsa 123");
        assertThat(pedido.categoria()).isEqualTo(categoria.getNombre());
        assertThat(pedido.libro()).isEqualTo(libro.getTitulo());
        assertThat(pedido.estado()).isEqualTo(EstadoPedido.PENDIENTE_DE_EMPAQUE);

        // 5. Sin empaquetar todavia no se puede emitir el remito.
        assertThatThrownBy(() -> remitoService.generarRemito(pedido.id()))
                .isInstanceOf(IllegalStateException.class);

        // 6. Empaquetado: ahora si sale el remito.
        avanzar(pedido.id(), EstadoPedido.EMPAQUETADO, null);
        RemitoResponse remito = remitoService.generarRemito(pedido.id());
        assertThat(remito.getNumero()).isEqualTo(pedido.id());
        assertThat(remito.getEdicion()).isEqualTo(edicion.getNombre());
        assertThat(remito.getNombreDestinatario()).isEqualTo("Suscriptora CIR");
        assertThat(remito.getLibro()).isEqualTo(libro.getTitulo());
        assertThat(remito.getEstado()).isEqualTo(EstadoPedido.EMPAQUETADO);

        // 7. Despachado (con tracking) y entregado.
        avanzar(pedido.id(), EstadoPedido.DESPACHADO, "TRK-CIR-001");
        PedidoDespachoResponse entregado = avanzar(pedido.id(), EstadoPedido.ENTREGADO, null);
        assertThat(entregado.estado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(entregado.tracking()).isEqualTo("TRK-CIR-001");
        assertThat(entregado.modificadoPor()).isEqualTo(admin.getEmail());

        // 8. No repeticion: a partir de ahora el sistema sabe que la suscriptora ya recibio este libro.
        assertThat(pedidoEdicionRepository
                .existsBySuscripcionSuscriptorIdAndCuraduriaLibroId(suscriptor.getId(), libro.getId()))
                .isTrue();
    }

    @Test
    void pagoSinValidar_laSuscripcionQuedaFueraDelCorte() {
        pagoService.registrarPago(pedidoDePago());

        ResumenCierreDTO resumen = cierreEdicionService.ejecutarCorte(edicion.getId());

        assertThat(resumen.getTotalPedidosGenerados()).isZero();
        assertThat(resumen.getSuscripcionesExcluidas()).contains(suscripcion.getId());
        assertThat(despachoService.listarPedidos(edicion.getId(), null)).isEmpty();
    }

    // helpers
    private CrearPagoRequest pedidoDePago() {
        CrearPagoRequest request = new CrearPagoRequest();
        request.setSuscripcionId(suscripcion.getId());
        request.setEdicionId(edicion.getId());
        request.setMonto(new BigDecimal("5000.00"));
        return request;
    }

    private PedidoDespachoResponse avanzar(Long pedidoId, EstadoPedido estado, String tracking) {
        return despachoService.cambiarEstado(pedidoId,
                new CambiarEstadoPedidoRequest(estado, tracking, null), admin.getEmail());
    }
}