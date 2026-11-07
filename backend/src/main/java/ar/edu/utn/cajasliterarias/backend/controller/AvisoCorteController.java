package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.PosibleRepeticionResponse;
import ar.edu.utn.cajasliterarias.backend.service.AvisoCorteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Aviso antes del corte (solo ADMIN, por la regla general de SecurityConfig).
 * Edicion inexistente -> 400; edicion cerrada -> 409 (GlobalExceptionHandler).
 */
@RestController
@RequestMapping("/api/ediciones")
@RequiredArgsConstructor
public class AvisoCorteController {

    private final AvisoCorteService avisoCorteService;

    @GetMapping("/{id}/posibles-repeticiones")
    public List<PosibleRepeticionResponse> posiblesRepeticiones(@PathVariable Long id) {
        return avisoCorteService.posiblesRepeticiones(id);
    }
}