package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contra la base real (MySQL local): la consulta de "no repeticion de titulos"
 * (existsBySuscripcionSuscriptorIdAndCuraduriaLibroId) recorre bien
 * pedido -> suscripcion -> suscriptor y pedido -> curaduria -> libro.
 */
@SpringBootTest
@Transactional
class LibroYaRecibidoIntegrationTest {

    @Autowired private PedidoEdicionRepository pedidoRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private SuscriptorRepository suscriptorRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private EdicionRepository edicionRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private CuraduriaEdicionRepository curaduriaRepository;

    private Suscriptor lectora;
    private Suscriptor otraLectora;
    private Libro libroRecibido;
    private Libro libroNuevo;

    @BeforeEach
    void setUp() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Categoria categoria = new Categoria();
        categoria.setNombre("Repeticion-IT-" + sufijo);
        categoriaRepository.save(categoria);

        libroRecibido = libro("Libro recibido " + sufijo);
        libroNuevo = libro("Libro nuevo " + sufijo);

        lectora = suscriptor("lectora-" + sufijo);
        otraLectora = suscriptor("otra-" + sufijo);

        Edicion edicion = new Edicion();
        edicion.setNombre("Edicion Repeticion IT " + sufijo);
        edicion.setFechaCorte(LocalDate.now().minusDays(10));
        edicion.setEstado(EstadoEdicion.CERRADA); // CERRADA para no chocar con una ABIERTA de pruebas manuales
        edicionRepository.save(edicion);

        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setEdicion(edicion);
        curaduria.setCategoria(categoria);
        curaduria.setLibro(libroRecibido);
        curaduria.setPrecioVigente(new BigDecimal("5000.00"));
        curaduria.setCupoMaximo(30);
        curaduriaRepository.save(curaduria);

        // Solo "lectora" recibio libroRecibido (tiene un pedido de esa edicion).
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(lectora);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaAlta(LocalDate.now().minusMonths(2));
        suscripcionRepository.save(suscripcion);

        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEdicion(edicion);
        pago.setMonto(new BigDecimal("5000.00"));
        pago.setFechaPago(LocalDate.now().minusDays(15));
        pago.setEstado(EstadoPago.VALIDADO);
        pagoRepository.save(pago);

        PedidoEdicion pedido = new PedidoEdicion();
        pedido.setSuscripcion(suscripcion);
        pedido.setEdicion(edicion);
        pedido.setPago(pago);
        pedido.setCuraduria(curaduria);
        pedido.setCategoriaCongelada(categoria.getNombre());
        pedido.setLibroCongelado(libroRecibido.getTitulo());
        pedido.setPrecioAplicado(new BigDecimal("5000.00"));
        pedido.setNombreDestinatario(lectora.getNombre());
        pedido.setDireccionEntrega(lectora.getDireccion());
        pedido.setEstadoPedido(EstadoPedido.ENTREGADO);
        pedido.setFechaCreacion(LocalDate.now().minusDays(10));
        pedidoRepository.saveAndFlush(pedido);
    }

    private Libro libro(String titulo) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        return libroRepository.save(libro);
    }

    private Suscriptor suscriptor(String alias) {
        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setNombre("Suscriptora " + alias);
        suscriptor.setEmail(alias + "@repeticion-it.com");
        suscriptor.setDireccion("Calle Falsa 123");
        suscriptor.setFechaRegistro(LocalDate.now().minusMonths(2));
        return suscriptorRepository.save(suscriptor);
    }

    @Test
    void suscriptoraQueYaRecibioElLibro_devuelveTrue() {
        assertThat(pedidoRepository.existsBySuscripcionSuscriptorIdAndCuraduriaLibroId(
                lectora.getId(), libroRecibido.getId())).isTrue();
    }

    @Test
    void libroQueNuncaRecibio_devuelveFalse() {
        assertThat(pedidoRepository.existsBySuscripcionSuscriptorIdAndCuraduriaLibroId(
                lectora.getId(), libroNuevo.getId())).isFalse();
    }

    @Test
    void elLibroLoRecibioOtraPersona_noCuentaParaEstaSuscriptora() {
        assertThat(pedidoRepository.existsBySuscripcionSuscriptorIdAndCuraduriaLibroId(
                otraLectora.getId(), libroRecibido.getId())).isFalse();
    }
}