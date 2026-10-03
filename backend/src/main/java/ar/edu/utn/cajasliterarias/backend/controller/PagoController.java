package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearPagoRequest;
import ar.edu.utn.cajasliterarias.backend.model.Pago;
import ar.edu.utn.cajasliterarias.backend.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * Registra un pago nuevo, en estado PENDIENTE.
     * POST /api/pagos
     */
    @PostMapping
    public ResponseEntity<Pago> registrar(@RequestBody CrearPagoRequest request) {
        Pago pago = pagoService.registrarPago(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    /**
     * Lista los pagos pendientes de validar (panel de conciliacion).
     * GET /api/pagos/pendientes
     */
    @GetMapping("/pendientes")
    public List<Pago> listarPendientes() {
        return pagoService.listarPendientes();
    }

    /**
     * Marca un pago como validado.
     * PATCH /api/pagos/{id}/validar
     */
    @PatchMapping("/{id}/validar")
    public ResponseEntity<Pago> validar(@PathVariable Long id) {
        Pago pago = pagoService.validarPago(id);
        return ResponseEntity.ok(pago);
    }
}