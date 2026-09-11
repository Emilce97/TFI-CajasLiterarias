package ar.edu.utn.cajasliterarias.backend.model;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoFaltante;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "demanda_edicion")
@Data
public class DemandaEdicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "edicion_id", nullable = false)
    private Edicion edicion;

    @ManyToOne
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    private Integer cantidadRequerida;
    private Integer cantidadRecibida;

    @Enumerated(EnumType.STRING)
    private EstadoFaltante estadoFaltante;
}