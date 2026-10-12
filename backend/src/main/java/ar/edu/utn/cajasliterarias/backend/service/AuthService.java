package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.LoginRequest;
import ar.edu.utn.cajasliterarias.backend.dto.LoginResponse;
import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import ar.edu.utn.cajasliterarias.backend.repository.AdministradoraRepository;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdministradoraRepository adminRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        Administradora admin = adminRepo.findByEmail(email).orElse(null);

        // Mismo mensaje si el email no existe, no tiene password o la password no coincide.
        if (admin == null
                || admin.getPasswordHash() == null
                || !encoder.matches(req.password(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        return new LoginResponse(jwtService.generar(email, "ADMIN"), email, admin.getNombre(), "ADMIN");
    }
}
