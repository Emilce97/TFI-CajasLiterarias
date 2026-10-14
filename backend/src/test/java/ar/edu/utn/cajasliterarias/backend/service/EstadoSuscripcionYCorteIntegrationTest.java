package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarCategoriaRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba contra la base real el flujo completo: pausa / baja + corte.
 * Verifica que lo pedido antes del corte afecte a la edición que se cierra y que lo pedido
 * desde el día del corte rija desde la edición siguiente.
 */
@SpringBootTest
@Transactional
class EstadoSuscripcionYCorteIntegrationTest {

    @Autowired private SuscripcionService suscripcionService;
    @Autowired private CierreEdicionService cierreEdicionService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private EdicionRepository edicionRepository;
    @Autowired private CuraduriaEdicionRepository curaduriaEdicionRepository;
    @Autowired private SuscriptorRepository suscriptorRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PedidoEdicionRepository pedidoEdicionRepository;
    @Autowired private Clock clock;

    private Categoria romance;
    private Categoria misterio;
    private Libro libroRomance;
    private Libro libroMisterio;
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

        romance = guardarCategoria("Romance-EST");
        misterio = guardarCategoria("Misterio-EST");
        libroRomance = guardarLibro("Libro Romance EST");
        libroMisterio = guardarLibro("Libro Misterio EST");

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setNombre("Suscriptora EST");
        suscriptor.setEmail("est-" + UUID.randomUUID() + "@test.com");
        suscriptor.setDireccion("Calle Falsa 123");
        suscriptor.setFechaRegistro(LocalDate.now(clock));
        suscriptorRepository.save(suscriptor);

        suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(romance);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaAlta(LocalDate.now(clock));
        suscripcionRepository.save(suscripcion);
    }

    @Test
    void pausaAntesDelCorte_laSuscripcionNoEntraAEstaEdicion() {
        Edicion edicion = abrirEdicionConPagoValidado(LocalDate.now(clock).plusDays(5));

        suscripcionService.pausar(suscripcion.getId());
        cierreEdicionService.ejecutarCorte(edicion.getId());

        assertThat(tienePedido(suscripcion)).isFalse();
        assertThat(estadoActual().getEstado()).isEqualTo(EstadoSuscripcion.PAUSADA);
    }

    @Test
    void pausaElDiaDelCorte_laSuscripcionEntraAEstaEdicionYQuedaPausadaDesdeLaSiguiente() {
        Edicion edicion = abrirEdicionConPagoValidado(LocalDate.now(clock));

        suscripcionService.pausar(suscripcion.getId());
        cierreEdicionService.ejecutarCorte(edicion.getId());

        assertThat(tienePedido(suscripcion)).isTrue();
        Suscripcion despues = estadoActual();
        assertThat(despues.getEstado()).isEqualTo(EstadoSuscripcion.PAUSADA);
        assertThat(despues.getProximoEstado()).isNull();
    }

    @Test
    void bajaElDiaDelCorte_conCambioDeCategoriaPendiente_entraAEstaEdicionYNoPromueveLaCategoria() {
        Edicion edicion = abrirEdicionConPagoValidado(LocalDate.now(clock));

        CambiarCategoriaRequest cambio = new CambiarCategoriaRequest();
        cambio.setCategoriaId(misterio.getId());
        suscripcionService.cambiarCategoria(suscripcion.getId(), cambio);
        suscripcionService.darDeBaja(suscripcion.getId());
        cierreEdicionService.ejecutarCorte(edicion.getId());

        assertThat(pedidoDe(suscripcion).getCategoriaCongelada()).isEqualTo(romance.getNombre());
        Suscripcion despues = estadoActual();
        assertThat(despues.getEstado()).isEqualTo(EstadoSuscripcion.BAJA);
        assertThat(despues.getFechaBaja()).isEqualTo(LocalDate.now(clock));
        assertThat(despues.getCategoria().getId()).isEqualTo(romance.getId());
        assertThat(despues.getProximaCategoria()).isNull();
    }

    // helpers
    private Suscripcion estadoActual() {
        return suscripcionRepository.findById(suscripcion.getId()).orElseThrow();
    }

    private boolean tienePedido(Suscripcion s) {
        return pedidoEdicionRepository.findAll().stream()
                .anyMatch(p -> p.getSuscripcion().getId().equals(s.getId()));
    }

    private PedidoEdicion pedidoDe(Suscripcion s) {
        return pedidoEdicionRepository.findAll().stream()
                .filter(p -> p.getSuscripcion().getId().equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se genero pedido para la suscripcion " + s.getId()));
    }

    private Edicion abrirEdicionConPagoValidado(LocalDate fechaCorte) {
        Edicion edicion = new Edicion();
        edicion.setNombre("Edicion EST " + UUID.randomUUID().toString().substring(0, 8));
        edicion.setFechaCorte(fechaCorte);
        edicion.setEstado(EstadoEdicion.ABIERTA);
        edicionRepository.save(edicion);

        guardarCuraduria(edicion, romance, libroRomance);
        guardarCuraduria(edicion, misterio, libroMisterio);

        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEdicion(edicion);
        pago.setMonto(new BigDecimal("5000.00"));
        pago.setFechaPago(LocalDate.now(clock));
        pago.setEstado(EstadoPago.VALIDADO);
        pago.setFechaValidacion(LocalDateTime.now(clock));
        pagoRepository.save(pago);

        return edicion;
    }

    private void guardarCuraduria(Edicion edicion, Categoria categoria, Libro libro) {
        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setEdicion(edicion);
        curaduria.setCategoria(categoria);
        curaduria.setLibro(libro);
        curaduria.setPrecioVigente(new BigDecimal("5000.00"));
        curaduria.setCupoMaximo(10);
        curaduriaEdicionRepository.save(curaduria);
    }

    private Categoria guardarCategoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return categoriaRepository.save(categoria);
    }

    private Libro guardarLibro(String titulo) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        return libroRepository.save(libro);
    }
}
