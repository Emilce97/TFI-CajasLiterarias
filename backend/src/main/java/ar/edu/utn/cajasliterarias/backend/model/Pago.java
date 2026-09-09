package ar.edu.utn.cajasliterarias.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pago")
@Data
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @ManyToOne
    @JoinColumn(name = "edicion_id", nullable = false)
    private Edicion edicion;

    private BigDecimal monto;
    private LocalDate fechaPago;

    // pendiente / validado
    private String estado;

    @ManyToOne
    @JoinColumn(name = "validado_por_admin_id")
    private Administradora validadoPor;

    private LocalDate fechaValidacion;
}