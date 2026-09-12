package ar.edu.utn.cajasliterarias.backend.model;

import ar.edu.utn.cajasliterarias.backend.enums.MotivoExclusion;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Tabla hermana de PedidoEdicion: registra, de forma trazable, las suscripciones ACTIVA
 * que NO ingresaron a una edicion en el corte (por no tener pago validado).
 * Una suscripcion entra como mucho una vez por edicion (UNIQUE), igual
 * que PedidoEdicion.
 */
@Entity
@Table(
        name = "exclusion_edicion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"suscripcion_id", "edicion_id"})
)
@Data
public class ExclusionEdicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @ManyToOne
    @JoinColumn(name = "edicion_id", nullable = false)
    private Edicion edicion;

    @Enumerated(EnumType.STRING)
    private MotivoExclusion motivo;

    private LocalDate fechaRegistro;
}