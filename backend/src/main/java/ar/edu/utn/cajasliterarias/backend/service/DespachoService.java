package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarEstadoPedidoRequest;
import ar.edu.utn.cajasliterarias.backend.dto.PedidoDespachoResponse;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;
import ar.edu.utn.cajasliterarias.backend.repository.AdministradoraRepository;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.PedidoEdicionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespachoService {

    private final PedidoEdicionRepository pedidoRepo;
    private final EdicionRepository edicionRepo;
    private final AdministradoraRepository adminRepo;
    private final Clock clock;

    /** Pedidos de una edición cerrada; si estado es null, trae todos. */
    public List<PedidoDespachoResponse> listarPedidos(Long edicionId, EstadoPedido estado) {
        Edicion edicion = edicionRepo.findById(edicionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la edición " + edicionId));
        if (edicion.getEstado() != EstadoEdicion.CERRADA) {
            throw new IllegalStateException("La edición todavía está abierta: los pedidos se generan en el corte");
        }

        List<PedidoEdicion> pedidos = (estado == null)
                ? pedidoRepo.findByEdicionIdOrderByIdAsc(edicionId)
                : pedidoRepo.findByEdicionIdAndEstadoPedidoOrderByIdAsc(edicionId, estado);

        return pedidos.stream().map(PedidoDespachoResponse::desde).toList();
    }

    /** Avanza el pedido exactamente un paso en el circuito de despacho. */
    @Transactional
    public PedidoDespachoResponse cambiarEstado(Long pedidoId, CambiarEstadoPedidoRequest req, String emailAdmin) {
        if (req == null || req.estado() == null) {
            throw new IllegalArgumentException("Indicá el estado al que pasa el pedido");
        }

        PedidoEdicion pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + pedidoId));

        EstadoPedido actual = pedido.getEstadoPedido();
        EstadoPedido siguiente = actual.siguiente();
        if (siguiente == null) {
            throw new IllegalStateException("El pedido ya fue entregado: no admite más cambios");
        }
        if (req.estado() != siguiente) {
            throw new IllegalStateException(
                    "El pedido está " + actual + ": solo puede pasar a " + siguiente);
        }

        if (req.estado() == EstadoPedido.DESPACHADO) {
            if (req.tracking() == null || req.tracking().isBlank()) {
                throw new IllegalArgumentException("Para despachar hace falta el número de tracking");
            }
            pedido.setTracking(req.tracking().trim());
        }
        if (req.observaciones() != null && !req.observaciones().isBlank()) {
            pedido.setObservaciones(req.observaciones().trim());
        }

        Administradora admin = adminRepo.findByEmail(emailAdmin)
                .orElseThrow(() -> new IllegalStateException("No se encontró la administradora de la sesión"));

        pedido.setEstadoPedido(req.estado());
        pedido.setModificadoPor(admin);
        pedido.setFechaModificacion(LocalDateTime.now(clock));

        return PedidoDespachoResponse.desde(pedidoRepo.save(pedido));
    }
}