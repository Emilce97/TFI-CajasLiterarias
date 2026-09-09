package ar.edu.utn.cajasliterarias.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(
        name = "curaduria_edicion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"edicion_id", "categoria_id"})
)
@Data
public class CuraduriaEdicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "edicion_id", nullable = false)
    private Edicion edicion;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    private BigDecimal precioVigente;

    // Cantidad maxima de suscriptores para esta categoria en esta edicion.
    // Validado en el alta/cambio de suscripcion, no en el corte
    // (ver Notas de Diseno del DER: cupos por tematica).
    private Integer cupoMaximo;
}