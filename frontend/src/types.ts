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