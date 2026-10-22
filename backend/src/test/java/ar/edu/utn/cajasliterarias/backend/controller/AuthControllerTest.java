package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.config.SecurityConfig;
import ar.edu.utn.cajasliterarias.backend.dto.LoginRequest;
import ar.edu.utn.cajasliterarias.backend.dto.LoginResponse;
import ar.edu.utn.cajasliterarias.backend.dto.RegistroRequest;
import ar.edu.utn.cajasliterarias.backend.exception.EmailYaRegistradoException;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void login_correcto_devuelve200ConToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("token-falso", "admin@test.com", "Admin", "ADMIN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-falso"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void login_credencialesIncorrectas_devuelve401ConMensaje() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autenticado"))
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }

    @Test
    void login_emailInvalido_devuelve400SinLlamarAlService() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-email\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void registro_correcto_devuelve201ConTokenYSinSesionPrevia() throws Exception {
        when(authService.registrar(any(RegistroRequest.class)))
                .thenReturn(new LoginResponse("token-falso", "ana@test.com", "Ana", "SUSCRIPTOR"));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana@test.com\",\"password\":\"clave1234\",\"direccion\":\"Calle Falsa 123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-falso"))
                .andExpect(jsonPath("$.rol").value("SUSCRIPTOR"));
    }

    @Test
    void registro_emailYaRegistrado_devuelve409() throws Exception {
        when(authService.registrar(any(RegistroRequest.class))).thenThrow(new EmailYaRegistradoException());

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana@test.com\",\"password\":\"clave1234\",\"direccion\":\"Calle Falsa 123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email ya registrado"));
    }

    @Test
    void registro_conPasswordDeMenosDe8Caracteres_devuelve400SinLlamarAlService() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana@test.com\",\"password\":\"corta12\",\"direccion\":\"Calle Falsa 123\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
