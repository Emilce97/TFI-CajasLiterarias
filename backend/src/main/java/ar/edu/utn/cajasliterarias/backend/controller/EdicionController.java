package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearEdicionRequest;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.service.EdicionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}