package ar.edu.utn.cajasliterarias.backend.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoPedidoTest {

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE_DE_EMPAQUE,false",
            "EMPAQUETADO,true",
            "DESPACHADO,true",
            "ENTREGADO,true"
    })
    void permiteRemito_soloDesdeEmpaquetado(EstadoPedido estado, boolean esperado) {
        assertEquals(esperado, estado.permiteRemito());
    }
}
