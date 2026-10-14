// Cliente HTTP compartido para hablar con el backend (Spring Boot).
// Se centraliza aca la URL base para no repetirla en cada pagina.
import { borrarSesion, obtenerSesion } from "../utils/session";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

function armarCabeceras(conCuerpo: boolean): Record<string, string> {
    const cabeceras: Record<string, string> = {};
    if (conCuerpo) cabeceras["Content-Type"] = "application/json";
    const token = obtenerSesion()?.token;
    if (token) cabeceras["Authorization"] = `Bearer ${token}`;
    return cabeceras;
}

// 401 en una ruta protegida: no hay sesion o el token vencio.
function sesionVencida(): never {
    borrarSesion();
    window.location.href = "/login.html";
    throw new Error("Sesión vencida o sin iniciar");
}

export async function apiGet<T>(path: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        headers: armarCabeceras(false),
    });
    if (response.status === 401) sesionVencida();
    if (!response.ok) {
        throw new Error(`Error al llamar a ${path}: ${response.status}`);
    }
    return response.json();
}

// body es opcional: validar un pago no manda nada, registrar una recepcion si.
export async function apiPatch<T>(path: string, body?: unknown): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: "PATCH",
        headers: armarCabeceras(body !== undefined),
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (response.status === 401) sesionVencida();
    if (!response.ok) {
        throw new Error(`Error al llamar a ${path}: ${response.status}`);
    }
    return response.json();
}