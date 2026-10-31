package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearEdicionRequest;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.service.EdicionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import ar.edu.utn.cajasliterarias.backend.model.CuraduriaEdicion;
import java.util.List;

@RestController
@RequestMapping("/api/ediciones")
public class EdicionController {

    private final EdicionService edicionService;

    public EdicionController(EdicionService edicionService) {
        this.edicionService = edicionService;
    }

    @PostMapping
    public ResponseEntity<Edicion> crearEdicion(@RequestBody CrearEdicionRequest request) {
        // Los errores de validacion (400) y de edicion ya abierta (409) los maneja GlobalExceptionHandler,
        // que responde con un JSON con "message": asi la pantalla puede mostrar el motivo real.
        Edicion edicion = edicionService.crearEdicion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(edicion);
    }

    /**
     * Lista todas las ediciones si quien consulta es ADMIN; al público, solo la abierta.
     * GET /api/ediciones
     */
    @GetMapping
    public List<Edicion> listarEdiciones(Authentication authentication) {
        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return edicionService.listarEdiciones(esAdmin);
    }

    /**
     * Lista las curadurias (categoria, libro, precio, cupo) de una edicion puntual.
     * GET /api/ediciones/{id}/curadurias
     */
    @GetMapping("/{id}/curadurias")
    public ResponseEntity<?> listarCuradurias(@PathVariable Long id) {
        try {
            List<CuraduriaEdicion> curadurias = edicionService.listarCuraduriasDeEdicion(id);
            return ResponseEntity.ok(curadurias);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}