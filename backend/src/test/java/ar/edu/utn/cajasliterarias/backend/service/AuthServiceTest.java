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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AdministradoraRepository adminRepo;
    @Mock private SuscriptorRepository suscriptorRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private JwtService jwtService;
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(adminRepo, suscriptorRepo, encoder, jwtService, clock);
    }

    private Administradora admin(String hash) {
        Administradora a = new Administradora();
        a.setNombre("Admin");
        a.setEmail("admin@test.com");
        a.setPasswordHash(hash);
        return a;
    }

    private Suscriptor suscriptor(String hash) {
        Suscriptor s = new Suscriptor();
        s.setNombre("Juana");
        s.setEmail("juana@test.com");
        s.setPasswordHash(hash);
        return s;
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

    @Test
    void loginSuscriptorCorrecto_devuelveTokenYRolSuscriptor() {
        when(suscriptorRepo.findByEmail("juana@test.com")).thenReturn(Optional.of(suscriptor("hash")));
        when(encoder.matches("clave1234", "hash")).thenReturn(true);
        when(jwtService.generar("juana@test.com", "SUSCRIPTOR")).thenReturn("token-falso");

        LoginResponse respuesta = authService.login(new LoginRequest("  Juana@Test.com ", "clave1234"));

        assertThat(respuesta.token()).isEqualTo("token-falso");
        assertThat(respuesta.email()).isEqualTo("juana@test.com");
        assertThat(respuesta.nombre()).isEqualTo("Juana");
        assertThat(respuesta.rol()).isEqualTo("SUSCRIPTOR");
    }

    @Test
    void loginSuscriptor_conPasswordIncorrecta_lanzaCredencialesIncorrectas() {
        when(suscriptorRepo.findByEmail("juana@test.com")).thenReturn(Optional.of(suscriptor("hash")));
        when(encoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("juana@test.com", "mala")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
        verifyNoInteractions(jwtService);
    }

    @Test
    void suscriptorSinPassword_noPuedeLoguearse_conElMismoError() {
        when(suscriptorRepo.findByEmail("juana@test.com")).thenReturn(Optional.of(suscriptor(null)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("juana@test.com", "cualquiera")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
        verifyNoInteractions(encoder, jwtService);
    }

    @Test
    void loginConPasswordDeMasDe72Bytes_lanzaElMismoErrorSinConsultarLaBase() {
        String larga = "ñ".repeat(40); // 40 caracteres = 80 bytes

        assertThatThrownBy(() -> authService.login(new LoginRequest("juana@test.com", larga)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");
        verifyNoInteractions(adminRepo, suscriptorRepo, encoder, jwtService);
    }

    @Test
    void registrarCorrecto_guardaSuscriptorConHashYDevuelveToken() {
        when(encoder.encode("clave1234")).thenReturn("hash-bcrypt");
        when(jwtService.generar("ana@test.com", "SUSCRIPTOR")).thenReturn("token-falso");

        LoginResponse respuesta = authService.registrar(
                new RegistroRequest(" Ana Díaz ", "  Ana@Test.com ", "clave1234", " Calle Falsa 123 "));

        ArgumentCaptor<Suscriptor> captor = ArgumentCaptor.forClass(Suscriptor.class);
        verify(suscriptorRepo).save(captor.capture());
        Suscriptor guardado = captor.getValue();
        assertThat(guardado.getEmail()).isEqualTo("ana@test.com");
        assertThat(guardado.getNombre()).isEqualTo("Ana Díaz");
        assertThat(guardado.getDireccion()).isEqualTo("Calle Falsa 123");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(guardado.getFechaRegistro()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(respuesta.rol()).isEqualTo("SUSCRIPTOR");
        assertThat(respuesta.token()).isEqualTo("token-falso");
    }

    @Test
    void registrar_conPasswordDeMasDe72Bytes_lanzaIllegalArgumentSinGuardar() {
        RegistroRequest req = new RegistroRequest("Ana", "ana@test.com", "ñ".repeat(40), "Calle 1");

        assertThatThrownBy(() -> authService.registrar(req)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(suscriptorRepo, adminRepo, encoder);
    }

    @Test
    void registrar_conEmailDeSuscriptorExistente_lanzaEmailYaRegistrado() {
        when(suscriptorRepo.findByEmail("ana@test.com")).thenReturn(Optional.of(new Suscriptor()));

        assertThatThrownBy(() -> authService.registrar(
                new RegistroRequest("Ana", "ana@test.com", "clave1234", "Calle 1")))
                .isInstanceOf(EmailYaRegistradoException.class);
        verify(suscriptorRepo, never()).save(any(Suscriptor.class));
    }

    @Test
    void registrar_conEmailDeAdministradora_lanzaEmailYaRegistrado() {
        when(adminRepo.findByEmail("admin@test.com")).thenReturn(Optional.of(admin("hash")));

        assertThatThrownBy(() -> authService.registrar(
                new RegistroRequest("Otra", "admin@test.com", "clave1234", "Calle 1")))
                .isInstanceOf(EmailYaRegistradoException.class);
        verify(suscriptorRepo, never()).save(any(Suscriptor.class));
    }

    @Test
    void registrar_dosRegistrosSimultaneos_laSegundaDaEmailYaRegistrado() {
        // La primera consulta no lo ve; el save choca con el UNIQUE; la segunda consulta ya lo ve.
        when(suscriptorRepo.findByEmail("ana@test.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Suscriptor()));
        when(encoder.encode("clave1234")).thenReturn("hash");
        when(suscriptorRepo.save(any(Suscriptor.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> authService.registrar(
                new RegistroRequest("Ana", "ana@test.com", "clave1234", "Calle 1")))
                .isInstanceOf(EmailYaRegistradoException.class);
    }

    @Test
    void registrar_otroErrorDeIntegridad_noSeDisfrazaDeEmailRepetido() {
        when(encoder.encode("clave1234")).thenReturn("hash");
        when(suscriptorRepo.save(any(Suscriptor.class))).thenThrow(new DataIntegrityViolationException("otro"));

        assertThatThrownBy(() -> authService.registrar(
                new RegistroRequest("Ana", "ana@test.com", "clave1234", "Calle 1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
