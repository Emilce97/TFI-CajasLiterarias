package ar.edu.utn.cajasliterarias.backend.integration;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import ar.edu.utn.cajasliterarias.backend.service.CierreEdicionService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Circuito del suscriptor de punta a punta, desde la API y con la base real, sin mocks:
 * registro -> login -> suscripción -> cambio de temática -> pago (y el historial "Mis cajas recibidas").
 * Corre dentro de una transacción que se revierte: la base queda como estaba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CircuitoSuscriptorIntegrationTest {

    private static final String PASSWORD = "clave-segura-123";

    @Autowired private MockMvc mockMvc;
    @Autowired private Clock clock;
    @Autowired private CierreEdicionService cierreEdicionService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private LibroRepository libroRepository;
    @Autowired private EdicionRepository edicionRepository;
    @Autowired private CuraduriaEdicionRepository curaduriaRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PedidoEdicionRepository pedidoRepository;

    private String sufijo;
    private Categoria romance;
    private Categoria misterio;
    private Libro libroRomance;
    private Libro libroMisterio;
    private Edicion edicion;

    @BeforeEach
    void setUp() {
        // Si hay una edición ABIERTA de pruebas manuales, se cierra dentro de esta transacción (se revierte).
        Edicion previa = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        if (previa != null) {
            previa.setEstado(EstadoEdicion.CERRADA);
            edicionRepository.save(previa);
        }

        sufijo = UUID.randomUUID().toString().substring(0, 8);
        romance = guardarCategoria("Romance-CS-" + sufijo);
        misterio = guardarCategoria("Misterio-CS-" + sufijo);
        libroRomance = guardarLibro("Libro Romance " + sufijo);
        libroMisterio = guardarLibro("Libro Misterio " + sufijo);

        // Faltan 5 días para el corte: los cambios rigen ya.
        edicion = guardarEdicion("Edicion CS " + sufijo, LocalDate.now(clock).plusDays(5), EstadoEdicion.ABIERTA);
        guardarCuraduria(edicion, romance, libroRomance);
        guardarCuraduria(edicion, misterio, libroMisterio);
    }

    @Test
    void circuitoCompleto_registroLoginSuscripcionCambioDeTematicaYPago() throws Exception {
        String email = "ana-" + sufijo + "@test.com";
        registrar(email);
        String token = login(email);
        long suscripcionId = suscribirse(token, romance);

        // Antes del corte el cambio rige ya (misterio no tiene ningún pedido previo de esta persona).
        cambiarTematica(token, suscripcionId, misterio)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(misterio.getId()));

        informarPago(token, suscripcionId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        // Un segundo pago de la misma suscripción en la misma edición se rechaza.
        informarPago(token, suscripcionId).andExpect(status().isConflict());

        // Lo que quedó en la base.
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId).orElseThrow();
        assertThat(suscripcion.getCategoria().getId()).isEqualTo(misterio.getId());
        assertThat(pagoRepository.findByEdicionId(edicion.getId())).hasSize(1);

        // Todavía no hubo corte: no tiene cajas.
        mockMvc.perform(get("/api/suscriptores/mis-cajas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cambioDeTematicaRechazado_siYaRecibioElLibro_yElHistorialLoMuestra() throws Exception {
        String email = "bea-" + sufijo + "@test.com";
        registrar(email);
        String token = login(email);
        long suscripcionId = suscribirse(token, romance);

        // Antes de este mes ya había recibido el libro de misterio.
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId).orElseThrow();
        sembrarPedidoAnterior(suscripcion, misterio, libroMisterio);

        cambiarTematica(token, suscripcionId, misterio)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString(libroMisterio.getTitulo())));

        // La suscripción sigue en su temática.
        assertThat(suscripcionRepository.findById(suscripcionId).orElseThrow().getCategoria().getId())
                .isEqualTo(romance.getId());

        // "Mis cajas recibidas" muestra el pedido anterior, sin datos internos.
        mockMvc.perform(get("/api/suscriptores/mis-cajas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].libro").value(libroMisterio.getTitulo()))
                .andExpect(jsonPath("$[0].estado").value("ENTREGADO"))
                .andExpect(jsonPath("$[0].pago").doesNotExist());
    }

    @Test
    void unSuscriptorNoPuedeCambiarLaTematicaDeOtra_yTampocoSinSesion() throws Exception {
        String emailA = "carla-" + sufijo + "@test.com";
        String emailB = "dana-" + sufijo + "@test.com";
        registrar(emailA);
        registrar(emailB);
        long suscripcionDeA = suscribirse(login(emailA), romance);

        cambiarTematica(login(emailB), suscripcionDeA, misterio).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/suscripciones/{id}/categoria", suscripcionDeA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCategoria(misterio)))
                .andExpect(status().isUnauthorized());

        assertThat(suscripcionRepository.findById(suscripcionDeA).orElseThrow().getCategoria().getId())
                .isEqualTo(romance.getId());
    }

    @Test
    void alCorte_siYaRecibioElLibroDeSuTematica_elPedidoSeGeneraMarcadoYNoSeExcluye() throws Exception {
        String email = "eva-" + sufijo + "@test.com";
        registrar(email);
        String token = login(email);
        long suscripcionId = suscribirse(token, romance);
        informarPago(token, suscripcionId).andExpect(status().isCreated());

        // La administradora valida el pago (acá directo en la base: ese circuito lo prueba el test de Emi).
        Pago pago = pagoRepository.findByEdicionId(edicion.getId()).get(0);
        pago.setEstado(EstadoPago.VALIDADO);
        pago.setFechaValidacion(LocalDateTime.now(clock));
        pagoRepository.save(pago);

        // Ya había recibido el libro de romance en una edición anterior: la curaduría de este mes lo repite.
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId).orElseThrow();
        sembrarPedidoAnterior(suscripcion, romance, libroRomance);

        cierreEdicionService.ejecutarCorte(edicion.getId());

        PedidoEdicion pedido = pedidoRepository.findByEdicionIdOrderByIdAsc(edicion.getId()).stream()
                .filter(p -> p.getSuscripcion().getId().equals(suscripcionId))
                .findFirst().orElseThrow();
        assertThat(pedido.isLibroRepetido()).isTrue();
        assertThat(pedido.getLibroCongelado()).isEqualTo(libroRomance.getTitulo());
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PENDIENTE_DE_EMPAQUE);

        // El suscriptor ve las dos cajas en su historial.
        mockMvc.perform(get("/api/suscriptores/mis-cajas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---- Pasos de la API ----

    private void registrar(String email) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Suscriptor %s","email":"%s","password":"%s","direccion":"Calle Falsa 123"}
                                """.formatted(sufijo, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("SUSCRIPTOR"));
    }

    private String login(String email) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("SUSCRIPTOR"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }

    private long suscribirse(String token, Categoria categoria) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/suscripciones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCategoria(categoria)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private ResultActions cambiarTematica(String token, long suscripcionId, Categoria categoria) throws Exception {
        return mockMvc.perform(patch("/api/suscripciones/{id}/categoria", suscripcionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCategoria(categoria)));
    }

    private ResultActions informarPago(String token, long suscripcionId) throws Exception {
        return mockMvc.perform(post("/api/pagos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"suscripcionId":%d,"edicionId":%d,"monto":5000}
                        """.formatted(suscripcionId, edicion.getId())));
    }

    private String cuerpoCategoria(Categoria categoria) {
        return """
                {"categoriaId":%d}
                """.formatted(categoria.getId());
    }

    // ---- Datos de base ----

    private Categoria guardarCategoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return categoriaRepository.save(c);
    }

    private Libro guardarLibro(String titulo) {
        Libro l = new Libro();
        l.setTitulo(titulo);
        return libroRepository.save(l);
    }

    private Edicion guardarEdicion(String nombre, LocalDate fechaCorte, EstadoEdicion estado) {
        Edicion e = new Edicion();
        e.setNombre(nombre);
        e.setFechaCorte(fechaCorte);
        e.setEstado(estado);
        return edicionRepository.save(e);
    }

    private CuraduriaEdicion guardarCuraduria(Edicion ed, Categoria categoria, Libro libro) {
        CuraduriaEdicion c = new CuraduriaEdicion();
        c.setEdicion(ed);
        c.setCategoria(categoria);
        c.setLibro(libro);
        c.setPrecioVigente(new BigDecimal("5000.00"));
        c.setCupoMaximo(30);
        return curaduriaRepository.save(c);
    }

    /** Deja un pedido ENTREGADO de una edición cerrada anterior: lo que "ya recibió" el suscriptor. */
    private void sembrarPedidoAnterior(Suscripcion suscripcion, Categoria categoria, Libro libro) {
        Edicion anterior = guardarEdicion("Edicion previa CS " + UUID.randomUUID().toString().substring(0, 8),
                LocalDate.now(clock).minusDays(30), EstadoEdicion.CERRADA);
        CuraduriaEdicion curaduria = guardarCuraduria(anterior, categoria, libro);

        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEdicion(anterior);
        pago.setMonto(new BigDecimal("5000.00"));
        pago.setFechaPago(LocalDate.now(clock).minusDays(35));
        pago.setEstado(EstadoPago.VALIDADO);
        pagoRepository.save(pago);

        PedidoEdicion pedido = new PedidoEdicion();
        pedido.setSuscripcion(suscripcion);
        pedido.setEdicion(anterior);
        pedido.setPago(pago);
        pedido.setCuraduria(curaduria);
        pedido.setCategoriaCongelada(categoria.getNombre());
        pedido.setLibroCongelado(libro.getTitulo());
        pedido.setPrecioAplicado(new BigDecimal("5000.00"));
        pedido.setNombreDestinatario("Suscriptor " + sufijo);
        pedido.setDireccionEntrega("Calle Falsa 123");
        pedido.setEstadoPedido(EstadoPedido.ENTREGADO);
        pedido.setFechaCreacion(LocalDate.now(clock).minusDays(30));
        pedidoRepository.saveAndFlush(pedido);
    }
}