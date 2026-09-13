package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.enums.*;
import ar.edu.utn.cajasliterarias.backend.exception.EdicionYaCerradaException;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class CierreEdicionService {

    private final EdicionRepository edicionRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final PagoRepository pagoRepository;
    private final CuraduriaEdicionRepository curaduriaEdicionRepository;
    private final PedidoEdicionRepository pedidoEdicionRepository;
    private final DemandaEdicionRepository demandaEdicionRepository;
    private final ExclusionEdicionRepository exclusionEdicionRepository;

    public CierreEdicionService(
            EdicionRepository edicionRepository,
            SuscripcionRepository suscripcionRepository,
            PagoRepository pagoRepository,
            CuraduriaEdicionRepository curaduriaEdicionRepository,
            PedidoEdicionRepository pedidoEdicionRepository,
            DemandaEdicionRepository demandaEdicionRepository,
            ExclusionEdicionRepository exclusionEdicionRepository
    ) {
        this.edicionRepository = edicionRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.curaduriaEdicionRepository = curaduriaEdicionRepository;
        this.pedidoEdicionRepository = pedidoEdicionRepository;
        this.demandaEdicionRepository = demandaEdicionRepository;
        this.exclusionEdicionRepository = exclusionEdicionRepository;
    }

    @Transactional
    public ResumenCierreDTO ejecutarCorte() {

        Edicion edicion = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        if (edicion == null) {
            throw new IllegalStateException("No hay ninguna edición abierta para cerrar.");
        }

        // Guarda de idempotencia: si ya se generaron pedidos para esta edición, el corte ya se ejecutó.
        if (pedidoEdicionRepository.existsByEdicionId(edicion.getId())) {
            throw new EdicionYaCerradaException(edicion.getId());
        }

        List<Suscripcion> candidatas = suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA);

        Map<Long, Pago> pagosPorSuscripcion = new HashMap<>();
        for (Pago pago : pagoRepository.findByEdicionId(edicion.getId())) {
                pagosPorSuscripcion.put(pago.getSuscripcion().getId(), pago);
        }

        Map<Long, CuraduriaEdicion> curaduriaPorCategoria = new HashMap<>();
        for (CuraduriaEdicion c : curaduriaEdicionRepository.findByEdicionId(edicion.getId())) {
            curaduriaPorCategoria.put(c.getCategoria().getId(), c);
        }

        List<PedidoEdicion> pedidosGenerados = new ArrayList<>();
        List<Long> suscripcionesExcluidas = new ArrayList<>();

        for (Suscripcion suscripcion : candidatas) {
            Pago pago = pagosPorSuscripcion.get(suscripcion.getId());

            if (pago == null) {
                registrarExclusion(suscripcion, edicion, MotivoExclusion.SIN_PAGO);
                suscripcionesExcluidas.add(suscripcion.getId());
                continue;
            }

            if (pago.getEstado() != EstadoPago.VALIDADO) {
                registrarExclusion(suscripcion, edicion, MotivoExclusion.PAGO_PENDIENTE);
                suscripcionesExcluidas.add(suscripcion.getId());
                continue;
            }

            CuraduriaEdicion curaduria = curaduriaPorCategoria.get(suscripcion.getCategoria().getId());
            if (curaduria == null) {
                registrarExclusion(suscripcion, edicion, MotivoExclusion.SIN_CURADURIA);
                suscripcionesExcluidas.add(suscripcion.getId());
                continue;
            }

            PedidoEdicion pedido = new PedidoEdicion();
            pedido.setSuscripcion(suscripcion);
            pedido.setEdicion(edicion);
            pedido.setPago(pago);
            pedido.setCuraduria(curaduria);
            pedido.setCategoriaCongelada(suscripcion.getCategoria().getNombre());
            pedido.setLibroCongelado(curaduria.getLibro().getTitulo());
            pedido.setPrecioAplicado(curaduria.getPrecioVigente());
            pedido.setDireccionEntrega(suscripcion.getSuscriptor().getDireccion());
            pedido.setEstadoPedido(EstadoPedido.PENDIENTE_DE_EMPAQUE);
            pedido.setFechaCreacion(LocalDate.now());

            pedidoEdicionRepository.save(pedido);
            pedidosGenerados.add(pedido);
        }

        for (Suscripcion suscripcion : candidatas) {
            if (suscripcion.getProximaCategoria() != null) {
                suscripcion.setCategoria(suscripcion.getProximaCategoria());
                suscripcion.setProximaCategoria(null);
                suscripcion.setFechaSolicitudCambio(null);
                suscripcionRepository.save(suscripcion);
            }
        }

        actualizarDemanda(edicion, pedidosGenerados);

        edicion.setEstado(EstadoEdicion.CERRADA);
        edicionRepository.save(edicion);

        ResumenCierreDTO resumen = new ResumenCierreDTO();
        resumen.setEdicionId(edicion.getId());
        resumen.setTotalPedidosGenerados(pedidosGenerados.size());
        resumen.setTotalExcluidos(suscripcionesExcluidas.size());
        resumen.setSuscripcionesExcluidas(suscripcionesExcluidas);

        return resumen;
    }

    private void registrarExclusion(Suscripcion suscripcion, Edicion edicion, MotivoExclusion motivo) {
        ExclusionEdicion exclusion = new ExclusionEdicion();
        exclusion.setSuscripcion(suscripcion);
        exclusion.setEdicion(edicion);
        exclusion.setMotivo(motivo);
        exclusion.setFechaRegistro(LocalDate.now());
        exclusionEdicionRepository.save(exclusion);
    }

    private void actualizarDemanda(Edicion edicion, List<PedidoEdicion> pedidosGenerados) {
        Map<Long, Integer> cantidadPorLibro = new HashMap<>();
        Map<Long, Libro> librosPorId = new HashMap<>();

        for (PedidoEdicion pedido : pedidosGenerados) {
            Libro libro = pedido.getCuraduria().getLibro();
            librosPorId.put(libro.getId(), libro);
            cantidadPorLibro.merge(libro.getId(), 1, Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : cantidadPorLibro.entrySet()) {
            DemandaEdicion demanda = new DemandaEdicion();
            demanda.setEdicion(edicion);
            demanda.setLibro(librosPorId.get(entry.getKey()));
            demanda.setCantidadRequerida(entry.getValue());
            demanda.setCantidadRecibida(0);
            demanda.setEstadoFaltante(EstadoFaltante.SIN_FALTANTE);
            demandaEdicionRepository.save(demanda);
        }
    }
}