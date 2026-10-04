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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba contra la base real el flujo completo: cambio de categoria + corte.
 * Los tests con mocks no detectan el desplazamiento de una edicion porque
 * dependen del orden de las operaciones dentro del cierre.
 */
@SpringBootTest
@Transactional
class CambioCategoriaYCorteIntegrationTest {

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

        romance = guardarCategoria("Romance-IT");
        misterio = guardarCategoria("Misterio-IT");
        libroRomance = guardarLibro("Libro Romance IT");
        libroMisterio = guardarLibro("Libro Misterio IT");

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setNombre("Suscriptora IT");
        suscriptor.setEmail("it-" + UUID.randomUUID() + "@test.com");
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
    void cambioAntesDelCorte_elPedidoDeEsaEdicionSaleConLaCategoriaNueva() {
        Edicion edicion = abrirEdicionConPagoValidado(LocalDate.now(clock).plusDays(5));

        suscripcionService.cambiarCategoria(suscripcion.getId(), cambioA(misterio));
        cierreEdicionService.ejecutarCorte();

        PedidoEdicion pedido = pedidoDe(suscripcion);
        assertThat(pedido.getEdicion().getId()).isEqualTo(edicion.getId());
        assertThat(pedido.getCategoriaCongelada()).isEqualTo(misterio.getNombre());
        assertThat(pedido.getLibroCongelado()).isEqualTo(libroMisterio.getTitulo());
        assertThat(pedido.getCuraduria().getCategoria().getId()).isEqualTo(misterio.getId());

        Suscripcion despues = suscripcionRepository.findById(suscripcion.getId()).orElseThrow();
        assertThat(despues.getCategoria().getId()).isEqualTo(misterio.getId());
        assertThat(despues.getProximaCategoria()).isNull();
    }

    @Test
    void cambioDespuesDelCorte_elPedidoCerradoNoCambiaYLaNuevaCategoriaAplicaDesdeLaSiguiente() {
        abrirEdicionConPagoValidado(LocalDate.now(clock).minusDays(1));

        suscripcionService.cambiarCategoria(suscripcion.getId(), cambioA(misterio));
        cierreEdicionService.ejecutarCorte();

        PedidoEdicion pedido = pedidoDe(suscripcion);
        assertThat(pedido.getCategoriaCongelada()).isEqualTo(romance.getNombre());
        assertThat(pedido.getLibroCongelado()).isEqualTo(libroRomance.getTitulo());

        // Despues del corte, la suscripcion queda en la categoria nueva para el proximo ciclo.
        Suscripcion despues = suscripcionRepository.findById(suscripcion.getId()).orElseThrow();
        assertThat(despues.getCategoria().getId()).isEqualTo(misterio.getId());
        assertThat(despues.getProximaCategoria()).isNull();
    }

    @Test
    void cambioEnLaVentanaSinEdicionAbierta_impactaEnLaSiguienteEdicion() {
        // El setUp deja la base sin edición ABIERTA: es la ventana entre un cierre y la apertura de la siguiente.
        suscripcionService.cambiarCategoria(suscripcion.getId(), cambioA(misterio));

        abrirEdicionConPagoValidado(LocalDate.now(clock).plusDays(5));
        cierreEdicionService.ejecutarCorte();

        assertThat(pedidoDe(suscripcion).getCategoriaCongelada()).isEqualTo(misterio.getNombre());
    }

    // helpers

    private Edicion abrirEdicionConPagoValidado(LocalDate fechaCorte) {
        Edicion edicion = new Edicion();
        edicion.setNombre("Edicion IT " + UUID.randomUUID().toString().substring(0, 8));
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
        pago.setFechaValidacion(LocalDate.now(clock));
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

    private CambiarCategoriaRequest cambioA(Categoria destino) {
        CambiarCategoriaRequest request = new CambiarCategoriaRequest();
        request.setCategoriaId(destino.getId());
        return request;
    }

    private PedidoEdicion pedidoDe(Suscripcion s) {
        return pedidoEdicionRepository.findAll().stream()
                .filter(p -> p.getSuscripcion().getId().equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se genero pedido para la suscripcion " + s.getId()));
    }
}
