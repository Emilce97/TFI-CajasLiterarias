package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.DemandaItemDTO;
import ar.edu.utn.cajasliterarias.backend.dto.RegistrarRecepcionRequest;
import ar.edu.utn.cajasliterarias.backend.service.DemandaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Demanda de libros por edicion, para armar los pedidos a proveedores.
 * Los errores (datos invalidos o inexistentes 400, edicion abierta 409) los traduce GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api")
public class DemandaController {

    private final DemandaService demandaService;

    public DemandaController(DemandaService demandaService) {
        this.demandaService = demandaService;
    }

    /**
     * GET /api/ediciones/{id}/demanda
     */
    @GetMapping("/ediciones/{id}/demanda")
    public List<DemandaItemDTO> listarDemanda(@PathVariable Long id) {
        return demandaService.listarDemandaDeEdicion(id);
    }

    /**
     * Registra el total de ejemplares recibidos de un libro.
     * PATCH /api/demanda/{id}/recepcion   body: { "cantidadRecibida": 8 }
     */
    @PatchMapping("/demanda/{id}/recepcion")
    public DemandaItemDTO registrarRecepcion(@PathVariable Long id,
                                             @RequestBody RegistrarRecepcionRequest request) {
        return demandaService.registrarRecepcion(id, request.getCantidadRecibida());
    }
}