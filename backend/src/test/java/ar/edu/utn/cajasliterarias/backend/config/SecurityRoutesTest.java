package ar.edu.utn.cajasliterarias.backend.config;

import ar.edu.utn.cajasliterarias.backend.controller.*;
import ar.edu.utn.cajasliterarias.backend.repository.CategoriaRepository;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.security.SuscripcionAcceso;
import ar.edu.utn.cajasliterarias.backend.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        CierreEdicionController.class,
        CategoriaController.class,
        PagoController.class,
        DemandaController.class,
        EdicionController.class,
        SuscripcionController.class,
        SuscriptorController.class,
        AuthController.class
})
@Import({SecurityConfig.class, CorsConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres-123",
        "app.jwt.expiration-minutes=60",
        "app.cors.allowed-origin=http://localhost:5173"
})
class SecurityRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CierreEdicionService cierreEdicionService;

    @MockitoBean
    private CategoriaRepository categoriaRepository;

    @MockitoBean
    private PagoService pagoService;

    @MockitoBean
    private DemandaService demandaService;

    @MockitoBean
    private EdicionService edicionService;

    @MockitoBean
    private SuscripcionService suscripcionService;

    @MockitoBean
    private SuscriptorService suscriptorService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean(name = "suscripcionAcceso")
    private SuscripcionAcceso acceso;

    @Test
    void rutaPublica_sinToken_devuelve200() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    void rutaDeAdmin_sinToken_devuelve401ConJson() throws Exception {
        mockMvc.perform(post("/api/ediciones/1/cierre"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void rutaDeAdmin_conTokenDeAdmin_pasaLaSeguridad() throws Exception {
        String token = jwtService.generar("admin@test.com", "ADMIN");

        mockMvc.perform(post("/api/ediciones/1/cierre")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rutaDeAdmin_conTokenAlterado_devuelve401() throws Exception {
        String token = jwtService.generar("admin@test.com", "ADMIN");

        mockMvc.perform(post("/api/ediciones/1/cierre")
                        .header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preflightCors_sinToken_esPermitido() throws Exception {
        mockMvc.perform(options("/api/ediciones/1/cierre")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/pagos/pendientes",
            "PATCH,/api/pagos/1/validar",
            "POST,/api/ediciones",
            "POST,/api/ediciones/1/cierre",
            "GET,/api/ediciones/1/demanda",
            "PATCH,/api/demanda/1/recepcion"
    })
    void rutasDeAdmin_sinToken_devuelven401(String metodo, String ruta) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OTRO")
    void rutaDeAdmin_conOtroRol_devuelve403() throws Exception {
        mockMvc.perform(post("/api/ediciones/1/cierre"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarEdiciones_sinToken_esPublico() throws Exception {
        mockMvc.perform(get("/api/ediciones"))
                .andExpect(status().isOk());
    }

    // Solo prueba que la ruta es pública (el service está mockeado).
    // Que no filtre por estado lo cubre EdicionServiceTest.
    @Test
    void curadurias_sinToken_esPublico() throws Exception {
        mockMvc.perform(get("/api/ediciones/1/curadurias"))
                .andExpect(status().isOk());
    }

    private static final String RUTAS_DE_SUSCRIPTOR = """
            POST,/api/suscripciones
            GET,/api/suscripciones/mis-suscripciones
            PATCH,/api/suscripciones/1/baja
            PATCH,/api/suscripciones/1/pausa
            PATCH,/api/suscripciones/1/reanudar
            PATCH,/api/suscripciones/1/categoria
            POST,/api/pagos
            GET,/api/suscriptores/mi-perfil
            GET,/api/suscriptores/mis-cajas
            PATCH,/api/suscriptores/mi-perfil
            """;

    @ParameterizedTest
    @CsvSource(textBlock = RUTAS_DE_SUSCRIPTOR)
    void rutasDeSuscriptor_sinToken_devuelven401(String metodo, String ruta) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource(textBlock = RUTAS_DE_SUSCRIPTOR)
    void rutasDeSuscriptor_conAdmin_devuelven403(String metodo, String ruta) throws Exception {
        String token = jwtService.generar("admin@test.com", "ADMIN");

        mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource(textBlock = RUTAS_DE_SUSCRIPTOR)
    void rutasDeSuscriptor_conSuscriptor_pasanLaSeguridad(String metodo, String ruta) throws Exception {
        when(acceso.esPropia(any(), any())).thenReturn(true);
        String token = jwtService.generar("juana@test.com", "SUSCRIPTOR");

        mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/pagos/pendientes",
            "PATCH,/api/pagos/1/validar",
            "POST,/api/ediciones",
            "POST,/api/ediciones/1/cierre",
            "GET,/api/ediciones/1/demanda",
            "PATCH,/api/demanda/1/recepcion"
    })
    void rutasDeAdmin_conSuscriptor_devuelven403(String metodo, String ruta) throws Exception {
        String token = jwtService.generar("juana@test.com", "SUSCRIPTOR");

        mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rutaNoListada_esSoloDeAdmin() throws Exception {
        String token = jwtService.generar("juana@test.com", "SUSCRIPTOR");

        mockMvc.perform(get("/api/ruta-que-todavia-no-existe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void registro_sinToken_noExigeSesion() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // 400 de validación, no 401
    }

    @Test
    void listarEdiciones_conTokenDeSuscriptor_esPublico() throws Exception {
        String token = jwtService.generar("juana@test.com", "SUSCRIPTOR");

        mockMvc.perform(get("/api/ediciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
