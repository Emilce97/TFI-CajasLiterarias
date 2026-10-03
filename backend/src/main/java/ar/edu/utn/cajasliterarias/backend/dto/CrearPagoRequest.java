package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Datos para registrar un pago, con estado PENDIENTE hasta que
 * una administradora lo valide manualmente.
 */
@Data
public class CrearPagoRequest {
    private Long suscripcionId;
    private Long edicionId;
    private BigDecimal monto;
}