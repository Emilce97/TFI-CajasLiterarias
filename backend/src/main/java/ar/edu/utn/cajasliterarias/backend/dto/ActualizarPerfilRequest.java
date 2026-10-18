package ar.edu.utn.cajasliterarias.backend.dto;

import lombok.Data;

/** Datos que el suscriptor puede cambiar de su perfil. Ambos son opcionales, pero al menos uno tiene que venir. */
@Data
public class ActualizarPerfilRequest {
    private String nombre;
    private String direccion;
}