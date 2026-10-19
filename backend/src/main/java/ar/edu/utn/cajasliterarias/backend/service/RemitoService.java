package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.RemitoResponse;
import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;
import ar.edu.utn.cajasliterarias.backend.repository.PedidoEdicionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Genera el remito de un pedido con los datos congelados en el corte (snapshot).
 * No consulta el suscriptor: si cambia su nombre o direccion despues, el remito no cambia.
 */
@Service
public class RemitoService {

    private final PedidoEdicionRepository pedidoEdicionRepository;
    private final Clock clock;

    public RemitoService(PedidoEdicionRepository pedidoEdicionRepository, Clock clock) {
        this.pedidoEdicionRepository = pedidoEdicionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RemitoResponse generarRemito(Long pedidoId) {
        PedidoEdicion pedido = pedidoEdicionRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pedido con id " + pedidoId));

        if (!pedido.getEstadoPedido().permiteRemito()) {
            throw new IllegalStateException("El pedido " + pedidoId
                    + " todavia no esta empaquetado: el remito se emite desde EMPAQUETADO en adelante.");
        }

        RemitoResponse remito = new RemitoResponse();
        remito.setNumero(pedido.getId());
        remito.setFechaEmision(LocalDate.now(clock));
        remito.setEdicion(pedido.getEdicion().getNombre());
        remito.setNombreDestinatario(pedido.getNombreDestinatario());
        remito.setDireccionEntrega(pedido.getDireccionEntrega());
        remito.setCategoria(pedido.getCategoriaCongelada());
        remito.setLibro(pedido.getLibroCongelado());
        remito.setEstado(pedido.getEstadoPedido());
        remito.setTracking(pedido.getTracking());
        return remito;
    }
}
