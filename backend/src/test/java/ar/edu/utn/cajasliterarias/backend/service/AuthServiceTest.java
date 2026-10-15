package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.LoginRequest;
import ar.edu.utn.cajasliterarias.backend.dto.LoginResponse;
import ar.edu.utn.cajasliterarias.backend.model.Administradora;
import ar.edu.utn.cajasliterarias.backend.repository.AdministradoraRepository;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AdministradoraRepository adminRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    private Administradora admin(String hash) {
        Administradora a = new Administradora();
        a.setNombre("Admin");
        a.setEmail("admin@test.com");
        a.setPasswordHash(hash);
        return a;
    }

    @Test
    void loginCorrecto_devuelveTokenYRolAdmin() {
        when(adminRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(admin("hash")));
        when(encoder.matches("secreta123", "hash")).thenReturn(true);
        when(jwtService.generar("admin@test.com", "ADMIN")).thenReturn("token-falso");

        LoginResponse respuesta = authService.login(new LoginRequest("admin@test.com", "secreta123"));

        assertThat(respuesta.token()).isEqualTo("token-falso");
        assertThat(respuesta.email()).isEqualTo("admin@test.com");
        assertThat(respuesta.nombre()).isEqualTo("Admin");
        assertThat(respuesta.rol()).isEqualTo("ADMIN");
    }

    @Test
    void loginNormalizaElEmail_espaciosYMayusculas() {
        when(adminRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(admin("hash")));
        when(encoder.matches("secreta123", "hash")).thenReturn(true);
        when(jwtService.generar("admin@test.com", "ADMIN")).thenReturn("token-falso");

        LoginResponse respuesta = authService.login(new LoginRequest("  ADMIN@Test.com ", "secreta123"));

        assertThat(respuesta.email()).isEqualTo("admin@test.com");
    }

    @Test
    void emailInexistente_lanzaCredencialesIncorrectas() {
        when(adminRepo.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@test.com", "x")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
        verifyNoInteractions(jwtService);
    }

    @Test
    void passwordIncorrecta_lanzaElMismoError() {
        when(adminRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(admin("hash")));
        when(encoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@test.com", "mala")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
        verifyNoInteractions(jwtService);
    }

    @Test
    void cuentaSinPassword_noPuedeLoguearse() {
        when(adminRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(admin(null)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@test.com", "cualquiera")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
        verifyNoInteractions(encoder, jwtService);
    }
}
