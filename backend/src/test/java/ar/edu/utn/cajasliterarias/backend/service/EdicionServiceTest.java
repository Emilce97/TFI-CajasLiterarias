package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearEdicionRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CuraduriaItemDTO;
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

    private CrearEdicionRequest requestValido() {
        CuraduriaItemDTO item = new CuraduriaItemDTO();
        item.setCategoriaId(1L);
        item.setLibroId(1L);
        item.setPrecioVigente(new BigDecimal("5000"));
        item.setCupoMaximo(30);

        CrearEdicionRequest request = new CrearEdicionRequest();
        request.setNombre("Octubre 2026");
        request.setFechaCorte(LocalDate.of(2026, 10, 21));
        request.setFechaDespachoDesde(LocalDate.of(2026, 11, 1));
        request.setFechaDespachoHasta(LocalDate.of(2026, 11, 5));
        request.setCuradurias(List.of(item, item, item, item));
        return request;
    }

    @Test
    void crearEdicion_conDatosValidos_guardaEdicionYCuatroCuradurias() {
        when(edicionRepository.existsByNombre("Octubre 2026")).thenReturn(false);

        Edicion edicionGuardada = new Edicion();
        edicionGuardada.setId(1L);
        when(edicionRepository.save(any(Edicion.class))).thenReturn(edicionGuardada);

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        Libro libro = new Libro();
        libro.setId(1L);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libro));

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
}