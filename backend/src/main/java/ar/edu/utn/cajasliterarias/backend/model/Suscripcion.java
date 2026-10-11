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

    // Pausa, reanudacion o baja pedida desde el dia del corte: no pisa "estado" hasta que
    // se ejecute el cierre, asi la suscripcion sigue contando en el padron de la edicion que se cierra.
    @Enumerated(EnumType.STRING)
    private EstadoSuscripcion proximoEstado;

    private LocalDate fechaSolicitudEstado;

    private LocalDate fechaAlta;
    private LocalDate fechaBaja;

    /**
     * Aplica la pausa/reanudación/baja pendiente, si la hay. Lo usa el service de suscripción
     * (cambio inmediato) y el cierre de edición (cambio pedido desde el día del corte).
     */
    public void promoverProximoEstado() {
        if (proximoEstado == null) {
            return;
        }
        estado = proximoEstado;
        if (estado == EstadoSuscripcion.BAJA) {
            fechaBaja = fechaSolicitudEstado;
            // una suscripción dada de baja no necesita un cambio de categoría pendiente
            proximaCategoria = null;
            fechaSolicitudCambio = null;
        }
        proximoEstado = null;
        fechaSolicitudEstado = null;
    }
}