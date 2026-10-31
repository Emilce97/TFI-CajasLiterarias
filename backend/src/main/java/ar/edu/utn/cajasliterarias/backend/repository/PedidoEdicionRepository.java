package ar.edu.utn.cajasliterarias.backend.repository;

import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoEdicionRepository extends JpaRepository<PedidoEdicion, Long> {

    // Guardado de idempotencia del corte: si ya hay algún PedidoEdicion generado para esta edición,
    // el corte ya se ejecutó y no debe reprocesarse.
    boolean existsByEdicionId(Long edicionId);

    // Panel de despacho: todos los pedidos de una edición.
    List<PedidoEdicion> findByEdicionIdOrderByIdAsc(Long edicionId);

    // Panel de despacho: pedidos de una edición en un estado puntual.
    List<PedidoEdicion> findByEdicionIdAndEstadoPedidoOrderByIdAsc(Long edicionId, EstadoPedido estadoPedido);

    // No repeticion de titulos: true si el suscriptor ya tiene algun pedido (de una edicion anterior)
    // con ese libro. Se compara por el libro de la curaduria, no por el titulo congelado.
    boolean existsBySuscripcionSuscriptorIdAndCuraduriaLibroId(Long suscriptorId, Long libroId);
}