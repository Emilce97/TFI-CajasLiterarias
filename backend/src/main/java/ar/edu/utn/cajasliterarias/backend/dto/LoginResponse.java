package ar.edu.utn.cajasliterarias.backend.dto;

public record LoginResponse(String token, String email, String nombre, String rol) {
}
