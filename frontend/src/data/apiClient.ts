// Cliente HTTP compartido para hablar con el backend (Spring Boot).
// Se centraliza aca la URL base para no repetirla en cada pagina.

export const API_BASE_URL = "http://localhost:8080/api";

export async function apiGet<T>(path: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${path}`);
    if (!response.ok) {
        throw new Error(`Error al llamar a ${path}: ${response.status}`);
    }
    return response.json();
}