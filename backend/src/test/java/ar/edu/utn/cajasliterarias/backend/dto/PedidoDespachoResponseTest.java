package ar.edu.utn.cajasliterarias.backend.dto;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.model.PedidoEdicion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoDespachoResponseTest {

    @Test
    void pedidoMarcadoEnElCorte_llegaAlPanelComoLibroRepetido() {
        PedidoEdicion pedido = pedido(true);

        assertThat(PedidoDespachoResponse.desde(pedido).libroRepetido()).isTrue();
    }

    @Test
    void pedidoSinMarca_noApareceComoRepetido() {
        PedidoEdicion pedido = pedido(false);

        assertThat(PedidoDespachoResponse.desde(pedido).libroRepetido()).isFalse();
    }

    private PedidoEdicion pedido(boolean repetido) {
        PedidoEdicion pedido = new PedidoEdicion();
        pedido.setId(1L);
        pedido.setNombreDestinatario("Suscriptor de prueba");
        pedido.setDireccionEntrega("Calle Falsa 123");
        pedido.setCategoriaCongelada("Romance");
        pedido.setLibroCongelado("Orgullo y prejuicio");
        pedido.setEstadoPedido(EstadoPedido.PENDIENTE_DE_EMPAQUE);
        pedido.setLibroRepetido(repetido);
        return pedido;
    }
}