package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearEdicionRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CuraduriaItemDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdicionServiceTest {

    @Mock private EdicionRepository edicionRepository;
    @Mock private CuraduriaEdicionRepository curaduriaEdicionRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private LibroRepository libroRepository;

    private EdicionService service;

    @BeforeEach
    void setUp() {
        service = new EdicionService(
                edicionRepository, curaduriaEdicionRepository, categoriaRepository, libroRepository
        );
    }

    private CuraduriaItemDTO crearItem(Long categoriaId, Long libroId) {
        CuraduriaItemDTO item = new CuraduriaItemDTO();
        item.setCategoriaId(categoriaId);
        item.setLibroId(libroId);
        item.setPrecioVigente(new BigDecimal("5000"));
        item.setCupoMaximo(30);
        return item;
    }

    private CrearEdicionRequest requestValido() {
        CrearEdicionRequest request = new CrearEdicionRequest();
        request.setNombre("Octubre 2026");
        request.setFechaCorte(LocalDate.of(2026, 10, 21));
        request.setFechaDespachoDesde(LocalDate.of(2026, 11, 1));
        request.setFechaDespachoHasta(LocalDate.of(2026, 11, 5));
        request.setCuradurias(List.of(
                crearItem(1L, 1L),
                crearItem(2L, 2L),
                crearItem(3L, 3L),
                crearItem(4L, 4L)
        ));
        return request;
    }

    @Test
    void crearEdicion_conDatosValidos_guardaEdicionYCuatroCuradurias() {
        when(edicionRepository.existsByNombre("Octubre 2026")).thenReturn(false);

        Edicion edicionGuardada = new Edicion();
        edicionGuardada.setId(1L);
        when(edicionRepository.save(any(Edicion.class))).thenReturn(edicionGuardada);

        for (long i = 1L; i <= 4L; i++) {
            Categoria categoria = new Categoria();
            categoria.setId(i);
            when(categoriaRepository.findById(i)).thenReturn(Optional.of(categoria));

            Libro libro = new Libro();
            libro.setId(i);
            when(libroRepository.findById(i)).thenReturn(Optional.of(libro));
        }

        Edicion resultado = service.crearEdicion(requestValido());

        assertEquals(1L, resultado.getId());
        verify(curaduriaEdicionRepository, times(4)).save(any(CuraduriaEdicion.class));
    }

    @Test
    void crearEdicion_conNombreDuplicado_lanzaExcepcion() {
        when(edicionRepository.existsByNombre("Octubre 2026")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crearEdicion(requestValido()));

        verify(edicionRepository, never()).save(any());
    }

    @Test
    void crearEdicion_conMenosDeCuatroCuradurias_lanzaExcepcion() {
        when(edicionRepository.existsByNombre("Octubre 2026")).thenReturn(false);

        CrearEdicionRequest request = requestValido();
        request.setCuradurias(request.getCuradurias().subList(0, 2)); // solo 2, deberian ser 4

        assertThrows(IllegalArgumentException.class, () -> service.crearEdicion(request));
    }

    @Test
    void crearEdicion_conCategoriaInexistente_lanzaExcepcion() {
        when(edicionRepository.existsByNombre("Octubre 2026")).thenReturn(false);

        Edicion edicionGuardada = new Edicion();
        edicionGuardada.setId(1L);
        when(edicionRepository.save(any(Edicion.class))).thenReturn(edicionGuardada);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.crearEdicion(requestValido()));
    }
    @Test
    void crearEdicion_conOtraEdicionYaAbierta_lanzaExcepcion() {
        when(edicionRepository.existsByNombre("Octubre 2026")).thenReturn(false);

        Edicion edicionYaAbierta = new Edicion();
        edicionYaAbierta.setId(99L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicionYaAbierta);

        assertThrows(IllegalArgumentException.class, () -> service.crearEdicion(requestValido()));

        verify(edicionRepository, never()).save(any());
    }
    @Test
    void crearEdicion_conCategoriaRepetida_lanzaExcepcion() {
        when(edicionRepository.existsByNombre("Octubre 2026")).thenReturn(false);

        CrearEdicionRequest request = requestValido();
        // Repetimos la categoria 1 en vez de usar la 4, como senalo Oscar
        request.setCuradurias(List.of(
                crearItem(1L, 1L),
                crearItem(1L, 2L),
                crearItem(2L, 3L),
                crearItem(3L, 4L)
        ));

        assertThrows(IllegalArgumentException.class, () -> service.crearEdicion(request));

        verify(edicionRepository, never()).save(any());
    }
}