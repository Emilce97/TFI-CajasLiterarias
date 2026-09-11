package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Datos que carga la administradora para UNA categoria dentro
 * de una edicion nueva (se envian 4 de estos, uno por categoria).
 */
@Data
public class CuraduriaItemDTO {

    private Long categoriaId;
    private Long libroId;
    private BigDecimal precioVigente;
    private Integer cupoMaximo;
}