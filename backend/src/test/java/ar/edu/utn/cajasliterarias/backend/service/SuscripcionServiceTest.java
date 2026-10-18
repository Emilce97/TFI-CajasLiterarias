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
    private CrearSuscripcionRequest requestAlta(Long categoriaId) {
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setCategoriaId(categoriaId);
        return request;
    }

    @Test
    void crear_sinCategoria_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta( null);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion("juana@mail.com", request));
    }

    @Test
    void crear_siYaTieneSuscripcionActivaEnLaCategoria_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta(10L);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);
        suscriptor.setEmail("juana@mail.com");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion("juana@mail.com", request));
    }

    @Test
    void crear_siSeAlcanzoElCupoMaximo_lanzaExcepcion() {
        CrearSuscripcionRequest request = requestAlta(10L);

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

        assertThrows(IllegalStateException.class, () -> service.crearSuscripcion("juana@mail.com", request));
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
        CrearSuscripcionRequest request = requestAlta(10L);
        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        edicionConCorteEn(0);

        assertThrows(IllegalStateException.class, () -> service.crearSuscripcion("juana@mail.com", request));
        verify(suscripcionRepository, never()).save(any(Suscripcion.class));
    }

    @Test
    void crear_antesDelCorte_creaLaSuscripcion() {
        CrearSuscripcionRequest request = requestAlta(10L);
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

        Suscripcion resultado = service.crearSuscripcion("juana@mail.com", request);

        assertEquals(EstadoSuscripcion.ACTIVA, resultado.getEstado());
    }

    @Test
    void crear_conElSuscriptorDeLaSesion_creaLaSuscripcionActivaYNoTocaSusDatos() {
        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(7L);
        suscriptor.setEmail("juana@mail.com");
        suscriptor.setNombre("Juana Diaz");
        suscriptor.setDireccion("Calle Falsa 123");

        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(7L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA)).thenReturn(2L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.crearSuscripcion("juana@mail.com", requestAlta(10L));

        assertEquals(suscriptor, resultado.getSuscriptor());
        assertEquals(EstadoSuscripcion.ACTIVA, resultado.getEstado());
        assertNotNull(resultado.getFechaAlta());
        // Nombre y dirección se cambian desde el perfil, no con el alta.
        assertEquals("Juana Diaz", suscriptor.getNombre());
        assertEquals("Calle Falsa 123", suscriptor.getDireccion());
        verify(suscriptorRepository, never()).save(any(Suscriptor.class));
    }

    @Test
    void crear_siElSuscriptorNoExiste_lanzaExcepcionSinCrearNada() {
        when(suscriptorRepository.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.crearSuscripcion("nadie@mail.com", requestAlta(10L)));
        verify(suscriptorRepository, never()).save(any(Suscriptor.class));
        verify(suscripcionRepository, never()).save(any(Suscripcion.class));
    }

    @Test
    void listarDelSuscriptor_devuelveLasDeEseEmail() {
        Suscripcion propia = new Suscripcion();
        propia.setId(1L);
        when(suscripcionRepository.findBySuscriptorEmail("juana@mail.com")).thenReturn(java.util.List.of(propia));

        assertEquals(java.util.List.of(propia), service.listarDelSuscriptor("juana@mail.com"));
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
