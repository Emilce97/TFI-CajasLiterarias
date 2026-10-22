// Cliente HTTP compartido para hablar con el backend (Spring Boot).
// Se centraliza aca la URL base para no repetirla en cada pagina.
import { borrarSesion, obtenerSesion } from "../utils/session";
import { irALogin, paginaActual } from "../utils/auth";

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
    irALogin(paginaActual());
    throw new Error("Sesión vencida o sin iniciar");
}

// Arma el error con el mensaje que mando el backend (ErrorResponse.message), si lo hay.
async function errorDeRespuesta(response: Response, path: string): Promise<Error> {
    try {
        const cuerpo = await response.json();
        if (typeof cuerpo?.message === "string" && cuerpo.message) return new Error(cuerpo.message);
    } catch {
        // Respuesta sin cuerpo JSON: se usa el mensaje generico.
    }
    return new Error(`Error al llamar a ${path}: ${response.status}`);
}

async function pedir<T>(method: string, path: string, body?: unknown): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers: armarCabeceras(body !== undefined),
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (response.status === 401) sesionVencida();
    if (!response.ok) throw await errorDeRespuesta(response, path);
    // 204 No Content (baja, pausa, reanudacion): no hay JSON que leer.
    if (response.status === 204) return undefined as T;
    return response.json();
}

export async function apiGet<T>(path: string): Promise<T> {
    return pedir<T>("GET", path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
    return pedir<T>("POST", path, body);
}

// body es opcional: validar un pago no manda nada, registrar una recepcion si.
export async function apiPatch<T>(path: string, body?: unknown): Promise<T> {
    return pedir<T>("PATCH", path, body);
}