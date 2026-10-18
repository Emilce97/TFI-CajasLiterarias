package ar.edu.utn.cajasliterarias.backend.security;

import ar.edu.utn.cajasliterarias.backend.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("suscripcionAcceso")
@RequiredArgsConstructor
public class SuscripcionAcceso {

    private final SuscripcionRepository repo;

    /** true solo si la suscripción existe y es del email indicado. */
    public boolean esPropia(Long suscripcionId, String email) {
        return suscripcionId != null && repo.findById(suscripcionId)
                .map(s -> s.getSuscriptor().getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }
}