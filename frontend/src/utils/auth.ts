import { borrarSesion, obtenerSesion } from "./session";

// Paginas a las que se puede volver despues de loguearse, por rol.
// Se comparan nombres exactos (nunca URLs) para no permitir redirecciones a otros sitios.
const PAGINAS_POR_ROL: Record<string, string[]> = {
    ADMIN: ["index.html"],
};

const INICIO_POR_ROL: Record<string, string> = {
    ADMIN: "index.html",
};

export function destinoTrasLogin(rol: string, next: string | null): string {
    return next && PAGINAS_POR_ROL[rol]?.includes(next)
        ? next
        : (INICIO_POR_ROL[rol] ?? "index.html");
}

export function tieneRol(rol: string): boolean {
    return obtenerSesion()?.rol === rol;
}

export function irALogin(next?: string): void {
    window.location.href = next ? `/login.html?next=${encodeURIComponent(next)}` : "/login.html";
}

/** Guard de las vistas protegidas: si no hay sesion con ese rol, manda al login. */
export function exigirRol(rol: string): boolean {
    if (tieneRol(rol)) return true;
    irALogin("index.html");
    return false;
}

export function cerrarSesion(): void {
    borrarSesion();
    window.location.href = "/index.html";
}