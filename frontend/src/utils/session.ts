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

// Lee la fecha de vencimiento (exp) que trae el JWT. Es solo comodidad de interfaz:
// el backend es quien valida el token de verdad.
function vencido(token: string): boolean {
    try {
        const payloadBase64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        const payload = JSON.parse(atob(payloadBase64));
        return typeof payload.exp === "number" && payload.exp * 1000 < Date.now();
    } catch {
        // Token ilegible: se trata como vencido.
        return true;
    }
}

export function obtenerSesion(): Sesion | null {
    try {
        const crudo = localStorage.getItem(CLAVE);
        if (!crudo) return null;

        const sesion = JSON.parse(crudo) as Sesion;
        if (vencido(sesion.token)) {
            localStorage.removeItem(CLAVE);
            return null;
        }
        return sesion;
    } catch {
        return null;
    }
}

export function borrarSesion(): void {
    localStorage.removeItem(CLAVE);
}