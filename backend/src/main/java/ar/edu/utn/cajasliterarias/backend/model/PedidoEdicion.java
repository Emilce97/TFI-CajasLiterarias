package ar.edu.utn.cajasliterarias.backend.model;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Snapshot inmutable (a nivel de negocio) generado al corte, solo para
 * suscripciones con estado = activa y pago validado. Congela categoria,
 * libro, precio y direccion vigentes en el momento del corte para que
 * cambios posteriores en Suscripcion o CuraduriaEdicion no alteren el
 * historico ya despachado.
 */
@Entity
@Table(name = "pedido_edicion")
@Data
public class PedidoEdicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    @ManyToOne
    @JoinColumn(name = "edicion_id", nullable = false)
    private Edicion edicion;

    @OneToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @ManyToOne
    @JoinColumn(name = "curaduria_id", nullable = false)
    private CuraduriaEdicion curaduria;

    // Datos congelados al momento del corte (redundancia intencional)
    private String categoriaCongelada;
    private String libroCongelado;
    private BigDecimal precioAplicado;
    private String direccionEntrega;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoPedido;

    private String tracking;
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "modificado_por_admin_id")
    private Administradora modificadoPor;

    private LocalDate fechaModificacion;
    private LocalDate fechaCreacion;
}