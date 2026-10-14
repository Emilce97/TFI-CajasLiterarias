package ar.edu.utn.cajasliterarias.backend.config;

import ar.edu.utn.cajasliterarias.backend.controller.CategoriaController;
import ar.edu.utn.cajasliterarias.backend.controller.CierreEdicionController;
import ar.edu.utn.cajasliterarias.backend.repository.CategoriaRepository;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.CierreEdicionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CierreEdicionController.class, CategoriaController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-prueba-de-al-menos-32-caracteres-123",
        "app.jwt.expiration-minutes=60"
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
}
