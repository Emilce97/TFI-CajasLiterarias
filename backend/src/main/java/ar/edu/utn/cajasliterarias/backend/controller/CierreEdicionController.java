package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.service.CierreEdicionService;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/cierre")
    public ResponseEntity<ResumenCierreDTO> cerrarEdicion() {
        ResumenCierreDTO resumen = cierreEdicionService.ejecutarCorte();
        return ResponseEntity.ok(resumen);
    }
}
