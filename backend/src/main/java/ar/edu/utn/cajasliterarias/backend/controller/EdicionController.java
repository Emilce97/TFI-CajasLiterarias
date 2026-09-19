package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearEdicionRequest;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.service.EdicionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<?> crearEdicion(@RequestBody CrearEdicionRequest request) {
        try {
            Edicion edicion = edicionService.crearEdicion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(edicion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Lista todas las ediciones existentes.
     * GET /api/ediciones
     */
    @GetMapping
    public List<Edicion> listarEdiciones() {
        return edicionService.listarEdiciones();
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