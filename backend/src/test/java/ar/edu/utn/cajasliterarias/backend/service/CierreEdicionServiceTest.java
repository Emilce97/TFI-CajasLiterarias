package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CierreEdicionServiceTest {

    @Mock private EdicionRepository edicionRepository;
    @Mock private SuscripcionRepository suscripcionRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private CuraduriaEdicionRepository curaduriaEdicionRepository;
    @Mock private PedidoEdicionRepository pedidoEdicionRepository;
    @Mock private DemandaEdicionRepository demandaEdicionRepository;

    private CierreEdicionService service;

    @BeforeEach
    void setUp() {
        service = new CierreEdicionService(
                edicionRepository, suscripcionRepository, pagoRepository,
                curaduriaEdicionRepository, pedidoEdicionRepository, demandaEdicionRepository
        );
    }

    @Test
    void siNoHayEdicionAbierta_lanzaExcepcion() {
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.ejecutarCorte());
    }

    @Test
    void suscripcionSinPagoValidado_quedaExcluidaYNoGeneraPedido() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setDireccion("Calle Falsa 123");

        Categoria categoria = new Categoria();
        categoria.setId(10L);
        categoria.setNombre("Misterio/Terror");

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(100L);
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);

        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA))
                .thenReturn(List.of(suscripcion));

        // No hay pagos cargados para esta edicion -> ninguno queda validado
        when(pagoRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());

        ResumenCierreDTO resumen = service.ejecutarCorte();

        assertEquals(0, resumen.getTotalPedidosGenerados());
        assertEquals(1, resumen.getTotalExcluidos());
        assertEquals(List.of(100L), resumen.getSuscripcionesExcluidas());
    }

    @Test
    void suscripcionConCambioPendiente_promocionaCategoriaDespuesDelCorte() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setDireccion("Calle Falsa 123");

        Categoria categoriaVigente = new Categoria();
        categoriaVigente.setId(10L);
        categoriaVigente.setNombre("Romance");

        Categoria categoriaProxima = new Categoria();
        categoriaProxima.setId(20L);
        categoriaProxima.setNombre("Misterio/Terror");

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(100L);
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoriaVigente);
        suscripcion.setProximaCategoria(categoriaProxima);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);

        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA))
                .thenReturn(List.of(suscripcion));

        // Sin pago validado a proposito: no es el foco de este test,
        // solo nos interesa comprobar que la promocion ocurre igual.
        when(pagoRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());

        service.ejecutarCorte();

        assertEquals(categoriaProxima, suscripcion.getCategoria());
        assertNull(suscripcion.getProximaCategoria());
        verify(suscripcionRepository).save(suscripcion);
    }
}