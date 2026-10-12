package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarCategoriaRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CrearSuscripcionRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock private SuscripcionRepository suscripcionRepository;
    @Mock private SuscriptorRepository suscriptorRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private EdicionRepository edicionRepository;
    @Mock private CuraduriaEdicionRepository curaduriaEdicionRepository;

    private SuscripcionService service;
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));

    @BeforeEach
    void setUp() {
        service = new SuscripcionService(
                suscripcionRepository, suscriptorRepository, categoriaRepository,
                edicionRepository, curaduriaEdicionRepository, clock
        );
    }

    private Categoria categoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private Edicion edicionAbierta() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        edicion.setEstado(EstadoEdicion.ABIERTA);
        return edicion;
    }

    private Edicion edicionAbiertaConCorte(LocalDate fechaCorte) {
        Edicion edicion = edicionAbierta();
        edicion.setFechaCorte(fechaCorte);
        return edicion;
    }

    private CuraduriaEdicion curaduria(Integer cupoMaximo) {
        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setCupoMaximo(cupoMaximo);
        return curaduria;
    }

    private CambiarCategoriaRequest request() {
        CambiarCategoriaRequest request = new CambiarCategoriaRequest();
        request.setCategoriaId(20L);
        return request;
    }

    // Crear una Suscripción
    private CrearSuscripcionRequest requestAlta(String nombre, String email, String direccion, Long categoriaId) {
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setNombre(nombre);
        request.setEmail(email);
        request.setDireccion(direccion);
        request.setCategoriaId(categoriaId);
        return request;
    }

    @Test
    void crear_sinEmail_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "  ", "Calle Falsa 123", 10L);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_conEmailFormatoInvalido_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana-arroba-mail", "Calle Falsa 123", 10L);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_sinNombre_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta(null, "juana@mail.com", "Calle Falsa 123", 10L);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_sinCategoria_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "Calle Falsa 123", null);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_siYaTieneSuscripcionActivaEnLaCategoria_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "Calle Falsa 123", 10L);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);
        suscriptor.setEmail("juana@mail.com");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_siSeAlcanzoElCupoMaximo_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "Calle Falsa 123", 10L);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);
        suscriptor.setEmail("juana@mail.com");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(5L);

        assertThrows(IllegalStateException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_conDatosValidosYSuscriptorNuevo_creaElSuscriptorYLaSuscripcionActiva() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "Calle Falsa 123", 10L);

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.empty());
        when(suscriptorRepository.save(any(Suscriptor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        // El suscriptor recién creado todavía no tiene id, por eso se stubea con null.
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(null, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(2L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.crearSuscripcion(request);

        verify(suscriptorRepository).save(any(Suscriptor.class));
        assertEquals("Juana Diaz", resultado.getSuscriptor().getNombre());
        assertEquals(EstadoSuscripcion.ACTIVA, resultado.getEstado());
        assertNotNull(resultado.getFechaAlta());
    }

    @Test
    void crear_conEmailYaRegistrado_reutilizaElSuscriptorExistenteSinCrearUnoNuevo() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "Calle Falsa 123", 10L);

        Suscriptor existente = new Suscriptor();
        existente.setId(7L);
        existente.setEmail("juana@mail.com");
        existente.setNombre("Juana Diaz");
        existente.setDireccion("Calle Falsa 123");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(7L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(2L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.crearSuscripcion(request);

        verify(suscriptorRepository, org.mockito.Mockito.never()).save(any(Suscriptor.class));
        assertEquals(existente, resultado.getSuscriptor());
    }
    @Test
    void crear_conEmailYaRegistradoYDatosNuevos_actualizaNombreYDireccion() {
        // Juana ya estaba registrada y se mudo: se suscribe a otra categoria con su direccion nueva.
        CrearSuscripcionRequest request = requestAlta("Juana Diaz de Perez", "juana@mail.com", "Av. Siempreviva 742", 10L);

        Suscriptor existente = new Suscriptor();
        existente.setId(7L);
        existente.setEmail("juana@mail.com");
        existente.setNombre("Juana Diaz");
        existente.setDireccion("Calle Falsa 123");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(7L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(2L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.crearSuscripcion(request);

        // Es la misma persona (mismo suscriptor), con los datos actualizados.
        assertEquals(existente, resultado.getSuscriptor());
        assertEquals("Juana Diaz de Perez", existente.getNombre());
        assertEquals("Av. Siempreviva 742", existente.getDireccion());
        verify(suscriptorRepository).save(existente);
    }

    @Test
    void crear_conEmailYaRegistradoYDireccionVacia_conservaLaDireccionAnterior() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "  ", 10L);

        Suscriptor existente = new Suscriptor();
        existente.setId(7L);
        existente.setEmail("juana@mail.com");
        existente.setNombre("Juana Diaz");
        existente.setDireccion("Calle Falsa 123");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(7L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(2L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.crearSuscripcion(request);

        // Un dato que llega vacio no borra el que ya estaba; y como nada cambio, no se guarda.
        assertEquals("Calle Falsa 123", existente.getDireccion());
        verify(suscriptorRepository, never()).save(any(Suscriptor.class));
    }

    @Test
    void crear_conEmailYaRegistrado_siElAltaSeRechaza_noModificaLosDatos() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz de Perez", "juana@mail.com", "Av. Siempreviva 742", 10L);

        Suscriptor existente = new Suscriptor();
        existente.setId(7L);
        existente.setEmail("juana@mail.com");
        existente.setNombre("Juana Diaz");
        existente.setDireccion("Calle Falsa 123");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(7L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        // Cupo completo: el alta se rechaza.
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(5L);

        assertThrows(IllegalStateException.class, () -> service.crearSuscripcion(request));

        // Si el alta no se concreta, los datos del suscriptor quedan como estaban.
        assertEquals("Juana Diaz", existente.getNombre());
        assertEquals("Calle Falsa 123", existente.getDireccion());
        verify(suscriptorRepository, never()).save(any(Suscriptor.class));
    }

    // Dar de Baja una Suscripción
    @Test
    void baja_siNoExisteLaSuscripcion_lanzaExcepcion() {
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.darDeBaja(100L));
    }

    @Test
    void baja_siYaEstaDadaDeBaja_lanzaExcepcion() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.BAJA);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));

        assertThrows(IllegalStateException.class, () -> service.darDeBaja(100L));
    }

    @Test
    void baja_deSuscripcionActiva_quedaEnBajaYLimpiaElCambioPendiente() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setProximaCategoria(categoria(20L, "Misterio/Terror"));
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));

        service.darDeBaja(100L);

        assertEquals(EstadoSuscripcion.BAJA, suscripcion.getEstado());
        assertNotNull(suscripcion.getFechaBaja());
        assertNull(suscripcion.getProximaCategoria());
        assertNull(suscripcion.getFechaSolicitudCambio());
        verify(suscripcionRepository).save(suscripcion);
    }

    // Pausar una suscripción
    @Test
    void pausar_siNoEstaActiva_lanzaExcepcion() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PAUSADA);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));

        assertThrows(IllegalStateException.class, () -> service.pausar(100L));
    }

    @Test
    void pausar_deSuscripcionActiva_quedaPausada() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));

        service.pausar(100L);

        assertEquals(EstadoSuscripcion.PAUSADA, suscripcion.getEstado());
        verify(suscripcionRepository).save(suscripcion);
    }

    // Reanudar una Suscripción
    @Test
    void reanudar_siNoEstaPausada_lanzaExcepcion() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));

        assertThrows(IllegalStateException.class, () -> service.reanudar(100L));
    }

    @Test
    void reanudar_deSuscripcionPausada_quedaActiva() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PAUSADA);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));

        service.reanudar(100L);

        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
        verify(suscripcionRepository).save(suscripcion);
    }

    // Cambiar Categoria de una Suscripción
    @Test
    void cambiarCategoria_siNoEstaActiva_lanzaExcepcion() {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.PAUSADA);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));

        CambiarCategoriaRequest request = new CambiarCategoriaRequest();
        request.setCategoriaId(20L);

        assertThrows(IllegalStateException.class, () -> service.cambiarCategoria(100L, request));
    }

    @Test
    void cambiarCategoria_siEsLaMismaCategoriaActual_lanzaExcepcion() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoriaActual));

        CambiarCategoriaRequest request = new CambiarCategoriaRequest();
        request.setCategoriaId(10L);

        assertThrows(IllegalArgumentException.class, () -> service.cambiarCategoria(100L, request));
    }

    @Test
    void cambiarCategoria_sinEdicionAbierta_seAplicaDirecto() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Categoria categoriaDestino = categoria(20L, "Misterio/Terror");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);

        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoriaDestino));
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(null);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.cambiarCategoria(100L, request());

        assertEquals(categoriaDestino, resultado.getCategoria());
        assertNull(resultado.getProximaCategoria());
        assertNull(resultado.getFechaSolicitudCambio());
        verifyNoInteractions(curaduriaEdicionRepository);
    }

    @Test
    void cambiarCategoria_antesDelCorte_seAplicaDirectoYAfectaLaEdicionQueSeCierra() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Categoria categoriaDestino = categoria(20L, "Misterio/Terror");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);

        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoriaDestino));
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA))
                .thenReturn(edicionAbiertaConCorte(LocalDate.now(clock).plusDays(5)));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.cambiarCategoria(100L, request());

        assertEquals(categoriaDestino, resultado.getCategoria());
        assertNull(resultado.getProximaCategoria());
        assertNull(resultado.getFechaSolicitudCambio());
    }

    @Test
    void cambiarCategoria_despuesDelCorte_quedaPendienteYNoTocaLaCategoriaVigente() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Categoria categoriaDestino = categoria(20L, "Misterio/Terror");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);

        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoriaDestino));
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA))
                .thenReturn(edicionAbiertaConCorte(LocalDate.now(clock).minusDays(1)));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.cambiarCategoria(100L, request());

        assertEquals(categoriaActual, resultado.getCategoria());
        assertEquals(categoriaDestino, resultado.getProximaCategoria());
        assertEquals(LocalDate.now(clock), resultado.getFechaSolicitudCambio());
    }

    @Test
    void cambiarCategoria_elMismoDiaDelCorte_cuentaComoPosterior() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Categoria categoriaDestino = categoria(20L, "Misterio/Terror");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);

        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoriaDestino));
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA))
                .thenReturn(edicionAbiertaConCorte(LocalDate.now(clock)));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.cambiarCategoria(100L, request());

        assertEquals(categoriaActual, resultado.getCategoria());
        assertEquals(categoriaDestino, resultado.getProximaCategoria());
    }

    @Test
    void cambiarCategoria_antesDelCorte_reemplazaUnCambioPendienteAnterior() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Categoria categoriaPendiente = categoria(30L, "Narrativa/Drama");
        Categoria categoriaDestino = categoria(20L, "Misterio/Terror");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);
        suscripcion.setProximaCategoria(categoriaPendiente);
        suscripcion.setFechaSolicitudCambio(LocalDate.now(clock).minusDays(3));

        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoriaDestino));
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA))
                .thenReturn(edicionAbiertaConCorte(LocalDate.now(clock).plusDays(5)));
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.cambiarCategoria(100L, request());

        assertEquals(categoriaDestino, resultado.getCategoria());
        assertNull(resultado.getProximaCategoria());
        assertNull(resultado.getFechaSolicitudCambio());
    }
    
    // Alta, baja, pausa y reanudación según la fecha de corte
    private Suscripcion suscripcionEn(EstadoSuscripcion estado) {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(estado);
        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        return suscripcion;
    }

    private void edicionConCorteEn(int diasDesdeHoy) {
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA))
                .thenReturn(edicionAbiertaConCorte(LocalDate.now(clock).plusDays(diasDesdeHoy)));
    }

    @Test
    void crear_elDiaDelCorte_lanzaExcepcionPorVentanaDeAltasCerrada() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "Calle Falsa 123", 10L);
        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        edicionConCorteEn(0);

        assertThrows(IllegalStateException.class, () -> service.crearSuscripcion(request));
        verify(suscripcionRepository, never()).save(any(Suscripcion.class));
    }

    @Test
    void crear_antesDelCorte_creaLaSuscripcion() {
        CrearSuscripcionRequest request = requestAlta("Juana Diaz", "juana@mail.com", "Calle Falsa 123", 10L);
        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        edicionConCorteEn(5);
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(2L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.crearSuscripcion(request);

        assertEquals(EstadoSuscripcion.ACTIVA, resultado.getEstado());
    }

    @Test
    void pausar_antesDelCorte_quedaPausadaEnElActo() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.ACTIVA);
        edicionConCorteEn(5);

        service.pausar(100L);

        assertEquals(EstadoSuscripcion.PAUSADA, suscripcion.getEstado());
        assertNull(suscripcion.getProximoEstado());
    }

    @Test
    void pausar_elDiaDelCorte_quedaPendienteYSigueActivaParaEstaEdicion() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.ACTIVA);
        edicionConCorteEn(0);

        service.pausar(100L);

        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
        assertEquals(EstadoSuscripcion.PAUSADA, suscripcion.getProximoEstado());
        assertEquals(LocalDate.now(clock), suscripcion.getFechaSolicitudEstado());
    }

    @Test
    void reanudar_antesDelCorte_quedaActivaEnElActo() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.PAUSADA);
        edicionConCorteEn(5);

        service.reanudar(100L);

        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
        assertNull(suscripcion.getProximoEstado());
    }

    @Test
    void reanudar_elDiaDelCorte_quedaPendienteYSigueSiendoPausadaParaEstaEdicion() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.PAUSADA);
        edicionConCorteEn(0);

        service.reanudar(100L);

        assertEquals(EstadoSuscripcion.PAUSADA, suscripcion.getEstado());
        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getProximoEstado());
    }

    @Test
    void reanudar_conPausaPendiente_laCancela() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.ACTIVA);
        suscripcion.setProximoEstado(EstadoSuscripcion.PAUSADA);
        suscripcion.setFechaSolicitudEstado(LocalDate.now(clock));
        edicionConCorteEn(0);

        service.reanudar(100L);

        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
        assertNull(suscripcion.getProximoEstado());
        assertNull(suscripcion.getFechaSolicitudEstado());
    }

    @Test
    void baja_conPausaPendiente_laReemplazaPorLaBaja() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.ACTIVA);
        suscripcion.setProximoEstado(EstadoSuscripcion.PAUSADA);
        suscripcion.setFechaSolicitudEstado(LocalDate.now(clock).minusDays(1));
        edicionConCorteEn(0);

        service.darDeBaja(100L);

        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
        assertEquals(EstadoSuscripcion.BAJA, suscripcion.getProximoEstado());
        assertEquals(LocalDate.now(clock), suscripcion.getFechaSolicitudEstado());
    }

    @Test
    void baja_elDiaDelCorte_quedaPendienteYNoTocaElEstadoVigente() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.ACTIVA);
        edicionConCorteEn(0);

        service.darDeBaja(100L);

        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.getEstado());
        assertEquals(EstadoSuscripcion.BAJA, suscripcion.getProximoEstado());
        assertNull(suscripcion.getFechaBaja());
    }

    @Test
    void conBajaPendiente_noSePuedePausarReanudarNiDarDeBajaDeNuevo() {
        Suscripcion suscripcion = suscripcionEn(EstadoSuscripcion.ACTIVA);
        suscripcion.setProximoEstado(EstadoSuscripcion.BAJA);

        assertThrows(IllegalStateException.class, () -> service.pausar(100L));
        assertThrows(IllegalStateException.class, () -> service.reanudar(100L));
        assertThrows(IllegalStateException.class, () -> service.darDeBaja(100L));
    }
}
