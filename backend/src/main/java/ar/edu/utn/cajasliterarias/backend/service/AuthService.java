package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.LoginRequest;
import ar.edu.utn.cajasliterarias.backend.dto.LoginResponse;
import ar.edu.utn.cajasliterarias.backend.dto.RegistroRequest;
import ar.edu.utn.cajasliterarias.backend.exception.EmailYaRegistradoException;
import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.repository.AdministradoraRepository;
import ar.edu.utn.cajasliterarias.backend.repository.SuscriptorRepository;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdministradoraRepository adminRepo;
    private final SuscriptorRepository suscriptorRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final Clock clock;

    public LoginResponse login(LoginRequest req) {
        // BCrypt solo usa los primeros 72 bytes: una contraseña más larga no puede ser la guardada.
        if (req.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
        String email = req.email().trim().toLowerCase();

        Administradora admin = adminRepo.findByEmail(email).orElse(null);
        if (admin != null && coincide(req.password(), admin.getPasswordHash())) {
            return new LoginResponse(jwtService.generar(email, "ADMIN"), email, admin.getNombre(), "ADMIN");
        }

        Suscriptor suscriptor = suscriptorRepo.findByEmail(email).orElse(null);
        if (suscriptor != null && coincide(req.password(), suscriptor.getPasswordHash())) {
            return new LoginResponse(jwtService.generar(email, "SUSCRIPTOR"), email, suscriptor.getNombre(), "SUSCRIPTOR");
        }

        // Mismo mensaje si el email no existe, no tiene password o la password no coincide.
        throw new BadCredentialsException("Credenciales incorrectas");
    }

    public LoginResponse registrar(RegistroRequest req) {
        if (req.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña es demasiado larga.");
        }
        String email = req.email().trim().toLowerCase();
        if (suscriptorRepo.findByEmail(email).isPresent() || adminRepo.findByEmail(email).isPresent()) {
            throw new EmailYaRegistradoException();
        }

        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setNombre(req.nombre().trim());
        suscriptor.setEmail(email);
        suscriptor.setDireccion(req.direccion().trim());
        suscriptor.setFechaRegistro(LocalDate.now(clock));
        suscriptor.setPasswordHash(encoder.encode(req.password()));
        try {
            suscriptorRepo.save(suscriptor);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo email: gana el primero.
            if (suscriptorRepo.findByEmail(email).isPresent()) {
                throw new EmailYaRegistradoException();
            }
            throw e; // otro problema de integridad: no se disfraza de "email repetido"
        }
        return new LoginResponse(jwtService.generar(email, "SUSCRIPTOR"), email, suscriptor.getNombre(), "SUSCRIPTOR");
    }

    private boolean coincide(String raw, String hash) {
        return hash != null && encoder.matches(raw, hash); // hash null = cuenta sin password
    }
}
