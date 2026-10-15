package ar.edu.utn.cajasliterarias.backend.config;

import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import ar.edu.utn.cajasliterarias.backend.repository.AdministradoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicialInitializer implements CommandLineRunner {

    private final AdministradoraRepository repo;
    private final PasswordEncoder encoder;
    @Value("${app.admin.email:}") private String email;
    @Value("${app.admin.password:}") private String password;

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            log.warn("ADMIN_EMAIL o ADMIN_PASSWORD no definidas: no se crea la administradora inicial");
            return;
        }
        String mail = email.trim().toLowerCase();
        Administradora a = repo.findByEmail(mail).orElseGet(() -> {
            Administradora n = new Administradora();
            n.setNombre("Administradora");
            n.setEmail(mail);
            return n;
        });
        if (a.getPasswordHash() == null) {
            a.setPasswordHash(encoder.encode(password));
            repo.save(a);
        }
    }
}
