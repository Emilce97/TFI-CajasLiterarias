import { borrarSesion, obtenerSesion } from "./session";

// Paginas a las que se puede volver despues de loguearse, por rol.
// Se comparan nombres exactos (nunca URLs) para no permitir redirecciones a otros sitios.
const PAGINAS_POR_ROL: Record<string, string[]> = {
    ADMIN: ["index.html"],
    SUSCRIPTOR: ["index.html", "suscripcion.html"],
};

const INICIO_POR_ROL: Record<string, string> = {
    ADMIN: "index.html",
    SUSCRIPTOR: "index.html",
};

export function destinoTrasLogin(rol: string, next: string | null): string {
    // "suscripcion.html?categoriaId=3" -> se compara solo "suscripcion.html".
    const pagina = next?.split(/[?#]/)[0];
    return next && pagina && PAGINAS_POR_ROL[rol]?.includes(pagina)
        ? next
        : (INICIO_POR_ROL[rol] ?? "index.html");
}

export function tieneRol(rol: string): boolean {
    return obtenerSesion()?.rol === rol;
}

// Pagina actual (con su query), para volver a ella despues de loguearse.
export function paginaActual(): string {
    const pagina = window.location.pathname.split("/").pop() || "index.html";
    return pagina + window.location.search;
}

export function irALogin(next?: string): void {
    window.location.href = next ? `/login.html?next=${encodeURIComponent(next)}` : "/login.html";
}

/** Guard de las vistas protegidas: si no hay sesion con ese rol, manda al login. */
export function exigirRol(rol: string, next: string = "index.html"): boolean {
    if (tieneRol(rol)) return true;
    irALogin(next);
    return false;
}

export function cerrarSesion(): void {
    borrarSesion();
    window.location.href = "/index.html";
}