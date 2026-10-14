// Sesion de la administradora, guardada en el navegador (localStorage).
export interface Sesion {
    token: string;
    email: string;
    nombre: string;
    rol: string;
}

const CLAVE = "cajasSesion";

export function guardarSesion(sesion: Sesion): void {
    localStorage.setItem(CLAVE, JSON.stringify(sesion));
}

export function obtenerSesion(): Sesion | null {
    try {
        const crudo = localStorage.getItem(CLAVE);
        return crudo ? (JSON.parse(crudo) as Sesion) : null;
    } catch {
        return null;
    }
}

export function borrarSesion(): void {
    localStorage.removeItem(CLAVE);
}