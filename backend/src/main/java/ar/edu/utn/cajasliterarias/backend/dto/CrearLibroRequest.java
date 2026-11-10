package ar.edu.utn.cajasliterarias.backend.dto;

/** Alta de libro desde la pantalla de Catalogo. Autor y proveedor son opcionales. */
public record CrearLibroRequest(String titulo, String autor, Long proveedorId) {
}