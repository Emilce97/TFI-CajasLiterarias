package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos que envia la administradora para configurar una edicion nueva:
 * los datos generales de la edicion, mas la curaduria de las 4 categorias.
 */
@Data
public class CrearEdicionRequest {

    private String nombre;
    private LocalDate fechaCorte;
    private LocalDate fechaDespachoDesde;
    private LocalDate fechaDespachoHasta;

    // Se espera que traiga exactamente 4 items, uno por cada categoria.
    private List<CuraduriaItemDTO> curadurias;
}