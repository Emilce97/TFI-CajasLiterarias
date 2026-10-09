package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.DemandaItemDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoFaltante;
import ar.edu.utn.cajasliterarias.backend.model.DemandaEdicion;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import ar.edu.utn.cajasliterarias.backend.repository.DemandaEdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandaServiceTest {

    @Mock private DemandaEdicionRepository demandaEdicionRepository;
    @Mock private EdicionRepository edicionRepository;

    private DemandaService service;

    @BeforeEach
    void setUp() {
        service = new DemandaService(demandaEdicionRepository, edicionRepository);
    }

    // helpers

    private Edicion edicion(EstadoEdicion estado) {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        edicion.setEstado(estado);
        return edicion;
    }

    private Proveedor proveedor(Long id, String nombre) {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(id);
        proveedor.setNombre(nombre);
        return proveedor;
    }

    private DemandaEdicion demanda(Long id, String titulo, Proveedor proveedor, Integer requerida, Integer recibida) {
        Libro libro = new Libro();
        libro.setId(id * 10);
        libro.setTitulo(titulo);
        libro.setAutor("Autor " + titulo);
        libro.setProveedor(proveedor);

        DemandaEdicion demanda = new DemandaEdicion();
        demanda.setId(id);
        demanda.setLibro(libro);
        demanda.setCantidadRequerida(requerida);
        demanda.setCantidadRecibida(recibida);
        demanda.setEstadoFaltante(EstadoFaltante.FALTANTE);
        return demanda;
    }

    @Test
    void listarDemanda_edicionCerrada_devuelveFilasConDatosDeLibroYProveedor() {
        Proveedor libreria = proveedor(1L, "Libreria Sur");
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));
        when(demandaEdicionRepository.findByEdicionId(1L))
                .thenReturn(List.of(demanda(5L, "Rayuela", libreria, 12, 0)));

        List<DemandaItemDTO> resultado = service.listarDemandaDeEdicion(1L);

        assertEquals(1, resultado.size());
        DemandaItemDTO item = resultado.get(0);
        assertEquals(5L, item.getId());
        assertEquals("Rayuela", item.getTitulo());
        assertEquals("Autor Rayuela", item.getAutor());
        assertEquals(1L, item.getProveedorId());
        assertEquals("Libreria Sur", item.getProveedorNombre());
        assertEquals(12, item.getCantidadRequerida());
        assertEquals(0, item.getCantidadRecibida());
        assertEquals(EstadoFaltante.FALTANTE, item.getEstadoFaltante());
    }

    @Test
    void listarDemanda_calculaLaCantidadFaltanteSinNegativos() {
        Proveedor libreria = proveedor(1L, "Libreria Sur");
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));
        when(demandaEdicionRepository.findByEdicionId(1L)).thenReturn(List.of(
                demanda(5L, "A", libreria, 10, 4),     // faltan 6
                demanda(6L, "B", libreria, 10, 15)));  // llegaron de mas: faltan 0, no -5

        List<DemandaItemDTO> resultado = service.listarDemandaDeEdicion(1L);

        assertEquals(6, resultado.get(0).getCantidadFaltante());
        assertEquals(0, resultado.get(1).getCantidadFaltante());
    }

    @Test
    void listarDemanda_ordenaPorProveedorYDespuesPorTitulo() {
        Proveedor alfa = proveedor(1L, "Alfa Libros");
        Proveedor beta = proveedor(2L, "Beta Distribuidora");
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));
        when(demandaEdicionRepository.findByEdicionId(1L)).thenReturn(List.of(
                demanda(1L, "Zama", beta, 3, 0),
                demanda(2L, "Ficciones", alfa, 3, 0),
                demanda(3L, "Boquitas pintadas", alfa, 3, 0)));

        List<String> titulos = service.listarDemandaDeEdicion(1L).stream()
                .map(DemandaItemDTO::getTitulo)
                .toList();

        assertEquals(List.of("Boquitas pintadas", "Ficciones", "Zama"), titulos);
    }

    @Test
    void listarDemanda_libroSinProveedor_muestraLeyendaEnVezDeFallar() {
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));
        when(demandaEdicionRepository.findByEdicionId(1L))
                .thenReturn(List.of(demanda(5L, "Rayuela", null, 12, 0)));

        DemandaItemDTO item = service.listarDemandaDeEdicion(1L).get(0);

        assertNull(item.getProveedorId());
        assertEquals("Sin proveedor asignado", item.getProveedorNombre());
    }

    @Test
    void listarDemanda_edicionSinDemanda_devuelveListaVacia() {
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.CERRADA)));
        when(demandaEdicionRepository.findByEdicionId(1L)).thenReturn(List.of());

        assertTrue(service.listarDemandaDeEdicion(1L).isEmpty());
    }

    @Test
    void listarDemanda_edicionAbierta_lanzaIllegalState() {
        when(edicionRepository.findById(1L)).thenReturn(Optional.of(edicion(EstadoEdicion.ABIERTA)));

        assertThrows(IllegalStateException.class, () -> service.listarDemandaDeEdicion(1L));
        verify(demandaEdicionRepository, never()).findByEdicionId(any());
    }

    @Test
    void listarDemanda_edicionInexistente_lanzaIllegalArgument() {
        when(edicionRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.listarDemandaDeEdicion(99L));
        assertTrue(ex.getMessage().contains("No existe la edicion"));
    }

    // registrarRecepcion

    @Test
    void registrarRecepcion_parcial_quedaFaltante() {
        DemandaEdicion fila = demanda(5L, "Rayuela", proveedor(1L, "Libreria Sur"), 10, 0);
        when(demandaEdicionRepository.findById(5L)).thenReturn(Optional.of(fila));
        when(demandaEdicionRepository.save(any(DemandaEdicion.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandaItemDTO resultado = service.registrarRecepcion(5L, 6);

        assertEquals(6, resultado.getCantidadRecibida());
        assertEquals(4, resultado.getCantidadFaltante());
        assertEquals(EstadoFaltante.FALTANTE, resultado.getEstadoFaltante());
    }

    @Test
    void registrarRecepcion_completa_quedaSinFaltante() {
        DemandaEdicion fila = demanda(5L, "Rayuela", proveedor(1L, "Libreria Sur"), 10, 0);
        when(demandaEdicionRepository.findById(5L)).thenReturn(Optional.of(fila));
        when(demandaEdicionRepository.save(any(DemandaEdicion.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandaItemDTO resultado = service.registrarRecepcion(5L, 10);

        assertEquals(0, resultado.getCantidadFaltante());
        assertEquals(EstadoFaltante.SIN_FALTANTE, resultado.getEstadoFaltante());
    }

    @Test
    void registrarRecepcion_reemplazaElTotalAnteriorEnVezDeSumar() {
        // Ya se habian cargado 8; la administradora corrige a 5 (se habia equivocado).
        DemandaEdicion fila = demanda(5L, "Rayuela", proveedor(1L, "Libreria Sur"), 10, 8);
        when(demandaEdicionRepository.findById(5L)).thenReturn(Optional.of(fila));
        when(demandaEdicionRepository.save(any(DemandaEdicion.class))).thenAnswer(inv -> inv.getArgument(0));

        DemandaItemDTO resultado = service.registrarRecepcion(5L, 5);

        assertEquals(5, resultado.getCantidadRecibida());
        assertEquals(5, resultado.getCantidadFaltante());
    }

    @Test
    void registrarRecepcion_conCantidadNull_lanzaIllegalArgumentYNoGuarda() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarRecepcion(5L, null));
        verify(demandaEdicionRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    void registrarRecepcion_conCantidadNegativa_lanzaIllegalArgumentYNoGuarda(int cantidad) {
        assertThrows(IllegalArgumentException.class, () -> service.registrarRecepcion(5L, cantidad));
        verify(demandaEdicionRepository, never()).save(any());
    }

    @Test
    void registrarRecepcion_demandaInexistente_lanzaIllegalArgument() {
        when(demandaEdicionRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrarRecepcion(99L, 3));
        assertTrue(ex.getMessage().contains("No existe la demanda"));
    }
}