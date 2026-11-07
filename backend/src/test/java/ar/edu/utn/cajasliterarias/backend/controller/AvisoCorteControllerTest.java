package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.config.SecurityConfig;
import ar.edu.utn.cajasliterarias.backend.dto.PosibleRepeticionResponse;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.AvisoCorteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvisoCorteController.class)
@Import(SecurityConfig.class)
class AvisoCorteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvisoCorteService avisoCorteService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_veLasPosiblesRepeticiones() throws Exception {
        when(avisoCorteService.posiblesRepeticiones(3L)).thenReturn(List.of(
                new PosibleRepeticionResponse(7L, "Lucía", "lucia@test.com", "Romance", "Orgullo y prejuicio")));

        mockMvc.perform(get("/api/ediciones/3/posibles-repeticiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].suscriptora").value("Lucía"))
                .andExpect(jsonPath("$[0].libro").value("Orgullo y prejuicio"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void edicionCerrada_devuelve409() throws Exception {
        when(avisoCorteService.posiblesRepeticiones(1L))
                .thenThrow(new IllegalStateException("La edición ya está cerrada: el aviso es solo antes del corte."));

        mockMvc.perform(get("/api/ediciones/1/posibles-repeticiones"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "SUSCRIPTOR")
    void suscriptora_noPuedeVerElAviso() throws Exception {
        mockMvc.perform(get("/api/ediciones/3/posibles-repeticiones"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(avisoCorteService);
    }

    @Test
    @WithAnonymousUser
    void sinLogin_devuelve401() throws Exception {
        mockMvc.perform(get("/api/ediciones/3/posibles-repeticiones"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(avisoCorteService);
    }
}