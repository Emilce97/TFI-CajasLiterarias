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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock private SuscripcionRepository suscripcionRepository;
    @Mock private SuscriptorRepository suscriptorRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private EdicionRepository edicionRepository;
    @Mock private CuraduriaEdicionRepository curaduriaEdicionRepository;

    private SuscripcionService service;

    @BeforeEach
    void setUp() {
        service = new SuscripcionService(
                suscripcionRepository, suscriptorRepository, categoriaRepository,
                edicionRepository, curaduriaEdicionRepository
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

    private CuraduriaEdicion curaduria(Integer cupoMaximo) {
        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setCupoMaximo(cupoMaximo);
        return curaduria;
    }

    // Crear una Suscripción
    @Test
    void crear_siNoExisteElSuscriptor_lanzaExcepcion() {
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setSuscriptorId(1L);
        request.setCategoriaId(10L);

        when(suscriptorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_siYaTieneSuscripcionActivaEnLaCategoria_lanzaExcepcion() {
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setSuscriptorId(1L);
        request.setCategoriaId(10L);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);

        when(suscriptorRepository.findById(1L)).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crearSuscripcion(request));
    }

    @Test
    void crear_siSeAlcanzoElCupoMaximo_lanzaExcepcion() {
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setSuscriptorId(1L);
        request.setCategoriaId(10L);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);

        when(suscriptorRepository.findById(1L)).thenReturn(Optional.of(suscriptor));
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
    void crear_conDatosValidos_creaLaSuscripcionActiva() {
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setSuscriptorId(1L);
        request.setCategoriaId(10L);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setId(1L);

        when(suscriptorRepository.findById(1L)).thenReturn(Optional.of(suscriptor));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Romance")));
        when(suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(1L, 10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(false);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 10L))
                .thenReturn(Optional.of(curaduria(5)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(10L, EstadoSuscripcion.ACTIVA))
                .thenReturn(2L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.crearSuscripcion(request);

        assertEquals(EstadoSuscripcion.ACTIVA, resultado.getEstado());
        assertNotNull(resultado.getFechaAlta());
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
    void cambiarCategoria_siSeAlcanzoElCupoDeLaCategoriaDestino_lanzaExcepcion() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Categoria categoriaDestino = categoria(20L, "Misterio/Terror");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);

        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoriaDestino));
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 20L))
                .thenReturn(Optional.of(curaduria(3)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(20L, EstadoSuscripcion.ACTIVA))
                .thenReturn(3L);

        CambiarCategoriaRequest request = new CambiarCategoriaRequest();
        request.setCategoriaId(20L);

        assertThrows(IllegalStateException.class, () -> service.cambiarCategoria(100L, request));
    }

    @Test
    void cambiarCategoria_conCupoDisponible_quedaComoProximaCategoriaSinPisarLaActual() {
        Categoria categoriaActual = categoria(10L, "Romance");
        Categoria categoriaDestino = categoria(20L, "Misterio/Terror");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setCategoria(categoriaActual);

        when(suscripcionRepository.findById(100L)).thenReturn(Optional.of(suscripcion));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoriaDestino));
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionAbierta());
        when(curaduriaEdicionRepository.findByEdicionIdAndCategoriaId(1L, 20L))
                .thenReturn(Optional.of(curaduria(3)));
        when(suscripcionRepository.countByCategoriaIdAndEstado(20L, EstadoSuscripcion.ACTIVA))
                .thenReturn(1L);
        when(suscripcionRepository.save(any(Suscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Suscripcion resultado = service.cambiarCategoria(100L, request());

        assertEquals(categoriaActual, resultado.getCategoria());
        assertEquals(categoriaDestino, resultado.getProximaCategoria());
        assertNotNull(resultado.getFechaSolicitudCambio());
    }

    private CambiarCategoriaRequest request() {
        CambiarCategoriaRequest request = new CambiarCategoriaRequest();
        request.setCategoriaId(20L);
        return request;
    }
}
