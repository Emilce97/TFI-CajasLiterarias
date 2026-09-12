package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoFaltante;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
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

    public CierreEdicionService(
            EdicionRepository edicionRepository,
            SuscripcionRepository suscripcionRepository,
            PagoRepository pagoRepository,
            CuraduriaEdicionRepository curaduriaEdicionRepository,
            PedidoEdicionRepository pedidoEdicionRepository,
            DemandaEdicionRepository demandaEdicionRepository
    ) {
        this.edicionRepository = edicionRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.pagoRepository = pagoRepository;
        this.curaduriaEdicionRepository = curaduriaEdicionRepository;
        this.pedidoEdicionRepository = pedidoEdicionRepository;
        this.demandaEdicionRepository = demandaEdicionRepository;
    }

    @Transactional
    public ResumenCierreDTO ejecutarCorte() {

        // TODO (Nati): reemplazar por la guarda de idempotencia real

        Edicion edicion = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        if (edicion == null) {
            throw new IllegalStateException("No hay ninguna edición abierta para cerrar.");
        }

        List<Suscripcion> candidatas = suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA);

        Map<Long, Pago> pagosValidadosPorSuscripcion = new HashMap<>();
        for (Pago pago : pagoRepository.findByEdicionId(edicion.getId())) {
            if (pago.getEstado() == EstadoPago.VALIDADO) {
                pagosValidadosPorSuscripcion.put(pago.getSuscripcion().getId(), pago);
            }
        }

        Map<Long, CuraduriaEdicion> curaduriaPorCategoria = new HashMap<>();
        for (CuraduriaEdicion c : curaduriaEdicionRepository.findByEdicionId(edicion.getId())) {
            curaduriaPorCategoria.put(c.getCategoria().getId(), c);
        }

        List<PedidoEdicion> pedidosGenerados = new ArrayList<>();
        List<Long> suscripcionesExcluidas = new ArrayList<>();

        for (Suscripcion suscripcion : candidatas) {
            Pago pagoValidado = pagosValidadosPorSuscripcion.get(suscripcion.getId());

            if (pagoValidado == null) {
                suscripcionesExcluidas.add(suscripcion.getId());
                continue;
            }

            CuraduriaEdicion curaduria = curaduriaPorCategoria.get(suscripcion.getCategoria().getId());
            if (curaduria == null) {
                suscripcionesExcluidas.add(suscripcion.getId());
                continue;
            }

            PedidoEdicion pedido = new PedidoEdicion();
            pedido.setSuscripcion(suscripcion);
            pedido.setEdicion(edicion);
            pedido.setPago(pagoValidado);
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