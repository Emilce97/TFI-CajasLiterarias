package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarEstadoPedidoRequest;
import ar.edu.utn.cajasliterarias.backend.dto.PedidoDespachoResponse;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.service.DespachoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Panel de despacho (solo ADMIN: estas rutas no están declaradas como públicas
 * en SecurityConfig, así que quedan protegidas por defecto).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DespachoController {

    private final DespachoService despachoService;

    /** GET /api/ediciones/{edicionId}/pedidos?estado=EMPAQUETADO (el estado es opcional) */
    @GetMapping("/ediciones/{edicionId}/pedidos")
    public List<PedidoDespachoResponse> listarPedidos(
            @PathVariable Long edicionId,
            @RequestParam(required = false) EstadoPedido estado) {
        return despachoService.listarPedidos(edicionId, estado);
    }

    /** PATCH /api/pedidos/{id}/estado con { "estado": ..., "tracking": ..., "observaciones": ... } */
    @PatchMapping("/pedidos/{id}/estado")
    public PedidoDespachoResponse cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambiarEstadoPedidoRequest request,
            Authentication authentication) {
        return despachoService.cambiarEstado(id, request, authentication.getName());
    }
}