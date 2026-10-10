// Tipos que reflejan la forma exacta del JSON que devuelve el backend.

export interface Edicion {
    id: number;
    nombre: string;
    fechaCorte: string;
    fechaDespachoDesde: string;
    fechaDespachoHasta: string;
    estado: "ABIERTA" | "CERRADA";
}

export interface Categoria {
    id: number;
    nombre: string;
}

export interface Libro {
    id: number;
    titulo: string;
}

export interface CuraduriaEdicion {
    id: number;
    categoria: Categoria;
    libro: Libro;
    precioVigente: number;
    cupoMaximo: number;
}
export interface Suscriptor {
    id: number;
    nombre: string;
    email: string;
    direccion: string;
}

export interface Suscripcion {
    id: number;
    estado: string;
    categoria: Categoria;
    suscriptor: Suscriptor;
}

export interface Pago {
    id: number;
    monto: number;
    fechaPago: string;
    fechaValidacion: string | null;
    estado: "PENDIENTE" | "VALIDADO";
    suscripcion: Suscripcion;
    edicion: Edicion;
}

// Una fila de la demanda de una edicion (GET /api/ediciones/{id}/demanda).
export interface DemandaItem {
    id: number;
    libroId: number;
    titulo: string;
    autor: string;
    proveedorId: number | null;
    proveedorNombre: string;
    cantidadRequerida: number;
    cantidadRecibida: number;
    cantidadFaltante: number;
    estadoFaltante: "FALTANTE" | "SIN_FALTANTE";
}