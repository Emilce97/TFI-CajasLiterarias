package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.service.CierreEdicionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ediciones")
public class CierreEdicionController {
    private final CierreEdicionService cierreEdicionService;

    public CierreEdicionController(CierreEdicionService cierreEdicionService) {
        this.cierreEdicionService = cierreEdicionService;
    }

    // El cierre se hace sobre una edicion puntual, identificada por id.
    @PostMapping("/{id}/cierre")
    public ResponseEntity<ResumenCierreDTO> cerrarEdicion(@PathVariable Long id) {
        ResumenCierreDTO resumen = cierreEdicionService.ejecutarCorte(id);
        return ResponseEntity.ok(resumen);
    }
}
