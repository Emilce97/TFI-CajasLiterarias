package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.config.SecurityConfig;
import ar.edu.utn.cajasliterarias.backend.dto.ActualizarPerfilRequest;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.SuscriptorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuscriptorController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "juana@mail.com", roles = "SUSCRIPTOR")
class SuscriptorControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SuscriptorService suscriptorService;
    @MockitoBean private JwtService jwtService;

    private Suscriptor juana() {
        Suscriptor s = new Suscriptor();
        s.setId(7L);
        s.setNombre("Juana Diaz");
        s.setEmail("juana@mail.com");
        s.setDireccion("Calle Falsa 123");
        s.setPasswordHash("$2a$10$hash-de-prueba");
        return s;
    }

    @Test
    void obtener_devuelveElPerfilSinElHash() throws Exception {
        when(suscriptorService.obtenerPerfil("juana@mail.com")).thenReturn(juana());

        mockMvc.perform(get("/api/suscriptores/mi-perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juana Diaz"))
                .andExpect(jsonPath("$.direccion").value("Calle Falsa 123"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void actualizar_conDireccion_devuelve200() throws Exception {
        when(suscriptorService.actualizarPerfil(eq("juana@mail.com"), any(ActualizarPerfilRequest.class)))
                .thenReturn(juana());

        mockMvc.perform(patch("/api/suscriptores/mi-perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccion\":\"Av. Siempreviva 742\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_sinNada_devuelve400ConMensaje() throws Exception {
        when(suscriptorService.actualizarPerfil(eq("juana@mail.com"), any(ActualizarPerfilRequest.class)))
                .thenThrow(new IllegalArgumentException("Indicá el nombre o la dirección que querés actualizar."));

        mockMvc.perform(patch("/api/suscriptores/mi-perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos inválidos"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtener_conRolAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/suscriptores/mi-perfil"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(suscriptorService);
    }
}
