package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.RemitoResponse;
import ar.edu.utn.cajasliterarias.backend.service.RemitoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Remito de un pedido. Solo ADMIN (lo cubre el anyRequest() de SecurityConfig).
 * Pedido inexistente 400 y pedido sin empaquetar 409: los traduce GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api")
public class RemitoController {

    private final RemitoService remitoService;

    public RemitoController(RemitoService remitoService) {
        this.remitoService = remitoService;
    }

    /**
     * GET /api/pedidos/{id}/remito
     */
    @GetMapping("/pedidos/{id}/remito")
    public RemitoResponse generarRemito(@PathVariable Long id) {
        return remitoService.generarRemito(id);
    }
}
