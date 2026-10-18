package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.ActualizarPerfilRequest;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.service.SuscriptorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil del suscriptor logueado. Siempre opera sobre la cuenta del token, nunca sobre un id recibido. */
@RestController
@RequestMapping("/api/suscriptores")
public class SuscriptorController {

    private final SuscriptorService suscriptorService;

    public SuscriptorController(SuscriptorService suscriptorService) {
        this.suscriptorService = suscriptorService;
    }

    @GetMapping("/mi-perfil")
    public Suscriptor obtener(Authentication authentication) {
        return suscriptorService.obtenerPerfil(authentication.getName());
    }

    @PatchMapping("/mi-perfil")
    public Suscriptor actualizar(@RequestBody ActualizarPerfilRequest request, Authentication authentication) {
        return suscriptorService.actualizarPerfil(authentication.getName(), request);
    }
}
