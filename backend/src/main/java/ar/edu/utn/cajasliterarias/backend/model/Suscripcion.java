package ar.edu.utn.cajasliterarias.backend.model;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "suscripcion")
@Data
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "suscriptor_id", nullable = false)
    private Suscriptor suscriptor;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    private EstadoSuscripcion estado;

    // Cambio de tematica solicitado despues del corte: no pisa "categoria"
    // hasta que se ejecute el proximo cierre (ver Notas de Diseno del DER).
    @ManyToOne
    @JoinColumn(name = "proxima_categoria_id")
    private Categoria proximaCategoria;

    private LocalDate fechaSolicitudCambio;
    private LocalDate fechaAlta;
    private LocalDate fechaBaja;
}