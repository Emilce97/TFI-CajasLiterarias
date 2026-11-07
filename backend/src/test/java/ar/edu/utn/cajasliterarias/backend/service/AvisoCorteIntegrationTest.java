package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.PosibleRepeticionResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aviso antes del corte contra la base real: detecta a la suscriptora que recibiria
 * un libro que ya recibio en una edicion anterior. Todo se revierte al terminar (@Transactional).
 */
@SpringBootTest
@Transactional
class AvisoCorteIntegrationTest {

    @Autowired private AvisoCorteService avisoCorteService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private EdicionRepository edicionRepository;
    @Autowired private CuraduriaEdicionRepository curaduriaEdicionRepository;
    @Autowired private SuscriptorRepository suscriptorRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PedidoEdicionRepository pedidoEdicionRepository;
    @Autowired private Clock clock;

    private Categoria categoria;
    private Libro libroYaRecibido;
    private Libro libroNuevo;
    private Suscripcion suscripcion;

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
        categoria.setNombre("Categoria AVI " + sufijo());
        categoriaRepository.save(categoria);

        libroYaRecibido = guardarLibro("Libro ya recibido AVI " + sufijo());
        libroNuevo = guardarLibro("Libro nuevo AVI " + sufijo());

        Suscriptor suscriptora = new Suscriptor();
        suscriptora.setNombre("Suscriptora AVI");
        suscriptora.setEmail("avi-" + UUID.randomUUID() + "@test.com");
        suscriptora.setDireccion("Calle Falsa 123");
        suscriptora.setFechaRegistro(LocalDate.now(clock));
        suscriptorRepository.save(suscriptora);

        suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptora);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaAlta(LocalDate.now(clock));
        suscripcionRepository.save(suscripcion);

        // Edicion anterior, ya cerrada, en la que la suscriptora recibio "libroYaRecibido".
        Edicion anterior = guardarEdicion(EstadoEdicion.CERRADA);
        CuraduriaEdicion curaduriaAnterior = guardarCuraduria(anterior, libroYaRecibido);
        guardarPedidoEntregado(anterior, curaduriaAnterior);
    }

    @Test
    void laNuevaEdicionRepiteElLibro_laSuscriptoraApareceEnElAviso() {
        Edicion nueva = guardarEdicion(EstadoEdicion.ABIERTA);
        guardarCuraduria(nueva, libroYaRecibido);

        List<PosibleRepeticionResponse> aviso = avisoCorteService.posiblesRepeticiones(nueva.getId());

        assertThat(aviso).hasSize(1);
        PosibleRepeticionResponse repeticion = aviso.get(0);
        assertThat(repeticion.suscripcionId()).isEqualTo(suscripcion.getId());
        assertThat(repeticion.suscriptora()).isEqualTo("Suscriptora AVI");
        assertThat(repeticion.categoria()).isEqualTo(categoria.getNombre());
        assertThat(repeticion.libro()).isEqualTo(libroYaRecibido.getTitulo());
    }

    @Test
    void laNuevaEdicionTraeOtroLibro_noHayAviso() {
        Edicion nueva = guardarEdicion(EstadoEdicion.ABIERTA);
        guardarCuraduria(nueva, libroNuevo);

        List<PosibleRepeticionResponse> aviso = avisoCorteService.posiblesRepeticiones(nueva.getId());

        assertThat(aviso).noneMatch(r -> r.suscripcionId().equals(suscripcion.getId()));
    }

    @Test
    void edicionCerrada_noSePideElAviso() {
        Edicion cerrada = guardarEdicion(EstadoEdicion.CERRADA);

        assertThatThrownBy(() -> avisoCorteService.posiblesRepeticiones(cerrada.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    // helpers
    private String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Libro guardarLibro(String titulo) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        return libroRepository.save(libro);
    }

    private Edicion guardarEdicion(EstadoEdicion estado) {
        Edicion edicion = new Edicion();
        edicion.setNombre("Edicion AVI " + sufijo());
        edicion.setFechaCorte(LocalDate.now(clock).plusDays(5));
        edicion.setEstado(estado);
        return edicionRepository.save(edicion);
    }

    private CuraduriaEdicion guardarCuraduria(Edicion edicion, Libro libro) {
        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setEdicion(edicion);
        curaduria.setCategoria(categoria);
        curaduria.setLibro(libro);
        curaduria.setPrecioVigente(new BigDecimal("5000.00"));
        curaduria.setCupoMaximo(10);
        return curaduriaEdicionRepository.save(curaduria);
    }

    private void guardarPedidoEntregado(Edicion edicion, CuraduriaEdicion curaduria) {
        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEdicion(edicion);
        pago.setMonto(new BigDecimal("5000.00"));
        pago.setFechaPago(LocalDate.now(clock));
        pago.setEstado(EstadoPago.VALIDADO);
        pago.setFechaValidacion(LocalDateTime.now(clock));
        pagoRepository.save(pago);

        PedidoEdicion pedido = new PedidoEdicion();
        pedido.setSuscripcion(suscripcion);
        pedido.setEdicion(edicion);
        pedido.setPago(pago);
        pedido.setCuraduria(curaduria);
        pedido.setCategoriaCongelada(categoria.getNombre());
        pedido.setLibroCongelado(curaduria.getLibro().getTitulo());
        pedido.setPrecioAplicado(curaduria.getPrecioVigente());
        pedido.setNombreDestinatario("Suscriptora AVI");
        pedido.setDireccionEntrega("Calle Falsa 123");
        pedido.setEstadoPedido(EstadoPedido.ENTREGADO);
        pedido.setFechaCreacion(LocalDate.now(clock));
        pedidoEdicionRepository.save(pedido);
    }
}