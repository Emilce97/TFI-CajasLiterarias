package ar.edu.utn.cajasliterarias.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "edicion")
@Data
public class Edicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private LocalDate fechaCorte;
    private LocalDate fechaDespachoDesde;
    private LocalDate fechaDespachoHasta;

    // abierta / cerrada
    private String estado;
}