package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.enums.MotivoExclusion;
import ar.edu.utn.cajasliterarias.backend.exception.EdicionYaCerradaException;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    @Mock private ExclusionEdicionRepository exclusionEdicionRepository;

    private CierreEdicionService service;
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));

    @BeforeEach
    void setUp() {
        service = new CierreEdicionService(
                edicionRepository, suscripcionRepository, pagoRepository,
                curaduriaEdicionRepository, pedidoEdicionRepository,
                demandaEdicionRepository, exclusionEdicionRepository, clock
        );
    }

    @Test
    void siNoHayEdicionAbierta_lanzaExcepcion() {
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.ejecutarCorte());
    }

    @Test
    void siLaEdicionYaFueCerrada_lanzaEdicionYaCerradaException() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(true);

        assertThrows(EdicionYaCerradaException.class, () -> service.ejecutarCorte());
    }

    @Test
    void suscripcionSinPago_quedaExcluidaConMotivoSinPago() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

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
        when(pagoRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());

        ResumenCierreDTO resumen = service.ejecutarCorte();

        assertEquals(0, resumen.getTotalPedidosGenerados());
        assertEquals(1, resumen.getTotalExcluidos());
        assertEquals(List.of(100L), resumen.getSuscripcionesExcluidas());
    }

    @Test
    void suscripcionConPagoNoValidado_quedaExcluidaConMotivoPagoPendiente() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setDireccion("Calle Falsa 123");

        Categoria categoria = new Categoria();
        categoria.setId(10L);
        categoria.setNombre("Romance");

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(100L);
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);

        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEstado(EstadoPago.PENDIENTE);

        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA))
                .thenReturn(List.of(suscripcion));
        when(pagoRepository.findByEdicionId(1L)).thenReturn(List.of(pago));
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());

        ResumenCierreDTO resumen = service.ejecutarCorte();

        assertEquals(0, resumen.getTotalPedidosGenerados());
        assertEquals(1, resumen.getTotalExcluidos());

        ArgumentCaptor<ExclusionEdicion> captor = ArgumentCaptor.forClass(ExclusionEdicion.class);
        verify(exclusionEdicionRepository).save(captor.capture());
        assertEquals(MotivoExclusion.PAGO_PENDIENTE, captor.getValue().getMotivo());
    }

    @Test
    void suscripcionSinCuraduriaParaSuCategoria_quedaExcluidaConMotivoSinCuraduria() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setDireccion("Calle Falsa 123");

        Categoria categoria = new Categoria();
        categoria.setId(10L);
        categoria.setNombre("Caja Sorpresa");

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(100L);
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);

        Pago pago = new Pago();
        pago.setSuscripcion(suscripcion);
        pago.setEstado(EstadoPago.VALIDADO);

        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA))
                .thenReturn(List.of(suscripcion));
        when(pagoRepository.findByEdicionId(1L)).thenReturn(List.of(pago));
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());

        ResumenCierreDTO resumen = service.ejecutarCorte();

        assertEquals(0, resumen.getTotalPedidosGenerados());
        assertEquals(1, resumen.getTotalExcluidos());

        ArgumentCaptor<ExclusionEdicion> captor = ArgumentCaptor.forClass(ExclusionEdicion.class);
        verify(exclusionEdicionRepository).save(captor.capture());
        assertEquals(MotivoExclusion.SIN_CURADURIA, captor.getValue().getMotivo());
    }

    @Test
    void suscripcionQueSuperaElCupoDeLaCategoria_quedaExcluidaConMotivoCupoCompleto() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

        Categoria romance = new Categoria();
        romance.setId(10L);
        romance.setNombre("Romance");

        Suscripcion primera = new Suscripcion();
        primera.setId(100L);
        primera.setCategoria(romance);
        primera.setEstado(EstadoSuscripcion.ACTIVA);
        primera.setSuscriptor(new Suscriptor());

        Suscripcion segunda = new Suscripcion();
        segunda.setId(200L);
        segunda.setCategoria(romance);
        segunda.setEstado(EstadoSuscripcion.ACTIVA);
        segunda.setSuscriptor(new Suscriptor());

        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA))
                .thenReturn(List.of(primera, segunda));

        Pago pago1 = new Pago();
        pago1.setSuscripcion(primera);
        pago1.setEstado(EstadoPago.VALIDADO);
        pago1.setFechaValidacion(LocalDate.of(2026, 9, 10));

        Pago pago2 = new Pago();
        pago2.setSuscripcion(segunda);
        pago2.setEstado(EstadoPago.VALIDADO);
        pago2.setFechaValidacion(LocalDate.of(2026, 9, 12));

        when(pagoRepository.findByEdicionId(1L)).thenReturn(List.of(pago1, pago2));

        Libro libro = new Libro();
        libro.setId(1L);
        libro.setTitulo("El Aleph");

        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setCategoria(romance);
        curaduria.setLibro(libro);
        curaduria.setCupoMaximo(1);
        curaduria.setPrecioVigente(BigDecimal.valueOf(5000));
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(List.of(curaduria));

        ResumenCierreDTO resumen = service.ejecutarCorte();

        assertEquals(1, resumen.getTotalPedidosGenerados());
        assertEquals(1, resumen.getTotalExcluidos());

        ArgumentCaptor<ExclusionEdicion> captor = ArgumentCaptor.forClass(ExclusionEdicion.class);
        verify(exclusionEdicionRepository).save(captor.capture());
        assertEquals(MotivoExclusion.CUPO_COMPLETO, captor.getValue().getMotivo());
        assertEquals(200L, captor.getValue().getSuscripcion().getId());
    }

    @Test
    void dosSuscripcionesCompitiendoPorElUltimoCupo_priorizaAQuienValidoElPagoAntes() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

        Categoria romance = new Categoria();
        romance.setId(10L);
        romance.setNombre("Romance");

        Suscripcion tardia = new Suscripcion();   // valida el pago DESPUES
        tardia.setId(100L);
        tardia.setCategoria(romance);
        tardia.setEstado(EstadoSuscripcion.ACTIVA);
        tardia.setSuscriptor(new Suscriptor());

        Suscripcion temprana = new Suscripcion(); // valida el pago ANTES
        temprana.setId(200L);
        temprana.setCategoria(romance);
        temprana.setEstado(EstadoSuscripcion.ACTIVA);
        temprana.setSuscriptor(new Suscriptor());

        // La devolvemos con la "tardia" primero en la lista,
        // para probar que el orden lo pone el sort y no el mock.
        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA))
                .thenReturn(List.of(tardia, temprana));

        Pago pagoTardio = new Pago();
        pagoTardio.setSuscripcion(tardia);
        pagoTardio.setEstado(EstadoPago.VALIDADO);
        pagoTardio.setFechaValidacion(LocalDate.of(2026, 9, 20));

        Pago pagoTemprano = new Pago();
        pagoTemprano.setSuscripcion(temprana);
        pagoTemprano.setEstado(EstadoPago.VALIDADO);
        pagoTemprano.setFechaValidacion(LocalDate.of(2026, 9, 10));

        when(pagoRepository.findByEdicionId(1L)).thenReturn(List.of(pagoTardio, pagoTemprano));

        Libro libro = new Libro();
        libro.setId(1L);
        libro.setTitulo("El Aleph");

        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setCategoria(romance);
        curaduria.setLibro(libro);
        curaduria.setCupoMaximo(1);
        curaduria.setPrecioVigente(BigDecimal.valueOf(5000));
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(List.of(curaduria));

        service.ejecutarCorte();

        ArgumentCaptor<PedidoEdicion> captor = ArgumentCaptor.forClass(PedidoEdicion.class);
        verify(pedidoEdicionRepository).save(captor.capture());
        assertEquals(temprana, captor.getValue().getSuscripcion());
    }

    @Test
    void suscripcionConCambioPendiente_promocionaCategoriaDespuesDelCorte() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

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
        when(suscripcionRepository.findByProximaCategoriaIsNotNull())
                .thenReturn(List.of(suscripcion));
        when(pagoRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());

        service.ejecutarCorte();

        assertEquals(categoriaProxima, suscripcion.getCategoria());
        assertNull(suscripcion.getProximaCategoria());
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    void suscripcionPausadaConCambioPendiente_tambienSePromueveEnElCorte() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

        Categoria categoriaVigente = new Categoria();
        categoriaVigente.setId(10L);
        categoriaVigente.setNombre("Romance");

        Categoria categoriaProxima = new Categoria();
        categoriaProxima.setId(20L);
        categoriaProxima.setNombre("Misterio/Terror");

        Suscripcion pausada = new Suscripcion();
        pausada.setId(101L);
        pausada.setCategoria(categoriaVigente);
        pausada.setProximaCategoria(categoriaProxima);
        pausada.setEstado(EstadoSuscripcion.PAUSADA);

        // Pausada: no es candidata del corte, pero sí tiene un cambio pendiente.
        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA)).thenReturn(Collections.emptyList());
        when(suscripcionRepository.findByProximaCategoriaIsNotNull()).thenReturn(List.of(pausada));
        when(pagoRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(Collections.emptyList());

        service.ejecutarCorte();

        assertEquals(categoriaProxima, pausada.getCategoria());
        assertNull(pausada.getProximaCategoria());
        assertNull(pausada.getFechaSolicitudCambio());
        verify(suscripcionRepository).save(pausada);
    }

    @Test
    void pedidoEdicionYaGenerado_noCambiaAunqueLaSuscripcionCambieDespues() {
        Edicion edicion = new Edicion();
        edicion.setId(1L);
        when(edicionRepository.findByEstado(EstadoEdicion.ABIERTA)).thenReturn(edicion);
        when(pedidoEdicionRepository.existsByEdicionId(1L)).thenReturn(false);

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setDireccion("Calle Falsa 123");

        Categoria romance = new Categoria();
        romance.setId(10L);
        romance.setNombre("Romance");

        Categoria misterio = new Categoria();
        misterio.setId(20L);
        misterio.setNombre("Misterio/Terror");

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setId(100L);
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(romance); // vigente al momento del corte
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);

        when(suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA))
                .thenReturn(List.of(suscripcion));

        Pago pagoValidado = new Pago();
        pagoValidado.setId(1L);
        pagoValidado.setSuscripcion(suscripcion);
        pagoValidado.setEstado(EstadoPago.VALIDADO);
        when(pagoRepository.findByEdicionId(1L)).thenReturn(List.of(pagoValidado));

        Libro libro = new Libro();
        libro.setId(1L);
        libro.setTitulo("El Aleph");

        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setCategoria(romance);
        curaduria.setLibro(libro);
        curaduria.setPrecioVigente(BigDecimal.valueOf(5000));
        when(curaduriaEdicionRepository.findByEdicionId(1L)).thenReturn(List.of(curaduria));

        // Ejecutamos el corte: en este momento la suscripcion todavia esta en Romance
        service.ejecutarCorte();

        // "Atrapamos" el PedidoEdicion que se guardo, para inspeccionarlo despues
        ArgumentCaptor<PedidoEdicion> captor = ArgumentCaptor.forClass(PedidoEdicion.class);
        verify(pedidoEdicionRepository).save(captor.capture());
        PedidoEdicion pedidoGenerado = captor.getValue();

        // El pedido debe quedar congelado en Romance ya en el momento del corte
        assertEquals("Romance", pedidoGenerado.getCategoriaCongelada());

        // Ahora, DESPUES del corte, alguien cambia la categoria de la suscripcion
        suscripcion.setCategoria(misterio);

        // La suscripcion (entidad continua) sí refleja el cambio
        assertEquals(misterio, suscripcion.getCategoria());
        // pero el pedido que ya se genero (snapshot inmutable) no se ve afectado
        assertEquals("Romance", pedidoGenerado.getCategoriaCongelada());
    }
}