package ar.edu.utn.cajasliterarias.backend.dto;

/** Alta de proveedor desde la pantalla de Catalogo. Contacto es opcional. */
public record CrearProveedorRequest(String nombre, String contacto) {
}