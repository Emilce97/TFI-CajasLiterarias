package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarCategoriaRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CrearSuscripcionRequest;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.service.SuscripcionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suscripciones")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    public SuscripcionController(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @PostMapping
    public ResponseEntity<Suscripcion> crear(@RequestBody CrearSuscripcionRequest request) {
        Suscripcion suscripcion = suscripcionService.crearSuscripcion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(suscripcion);
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        suscripcionService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/pausa")
    public ResponseEntity<Void> pausar(@PathVariable Long id) {
        suscripcionService.pausar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reanudar")
    public ResponseEntity<Void> reanudar(@PathVariable Long id) {
        suscripcionService.reanudar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/categoria")
    public ResponseEntity<Suscripcion> cambiarCategoria(
            @PathVariable Long id,
            @RequestBody CambiarCategoriaRequest request
    ) {
        Suscripcion suscripcion = suscripcionService.cambiarCategoria(id, request);
        return ResponseEntity.ok(suscripcion);
    }
}
