package ar.edu.utn.cajasliterarias.backend.enums;

public enum EstadoPedido {
    PENDIENTE_DE_EMPAQUE,
    EMPAQUETADO,
    DESPACHADO,
    ENTREGADO;

    /** Estado que sigue en el circuito de despacho, o null si ya es el último. */
    public EstadoPedido siguiente() {
        return switch (this) {
            case PENDIENTE_DE_EMPAQUE -> EMPAQUETADO;
            case EMPAQUETADO -> DESPACHADO;
            case DESPACHADO -> ENTREGADO;
            case ENTREGADO -> null;
        };
    }

    /** El remito se emite desde EMPAQUETADO en adelante: ahí la caja ya está armada. */
    public boolean permiteRemito() {
        return this.ordinal() >= EMPAQUETADO.ordinal();
    }
}