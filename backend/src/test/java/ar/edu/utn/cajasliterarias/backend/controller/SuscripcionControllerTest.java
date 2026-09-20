package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearSuscripcionRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.service.SuscripcionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuscripcionController.class)
class SuscripcionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SuscripcionService suscripcionService;

    private CrearSuscripcionRequest requestValido() {
        CrearSuscripcionRequest request = new CrearSuscripcionRequest();
        request.setNombre("Juana Diaz");
        request.setEmail("juana@mail.com");
        request.setDireccion("Calle Falsa 123");
        request.setCategoriaId(1L);
        return request;
    }

    @Test
    void crearSuscripcion_conDatosValidos_devuelve201() throws Exception {
        Suscripcion suscripcionCreada = new Suscripcion();
        suscripcionCreada.setId(1L);
        suscripcionCreada.setEstado(EstadoSuscripcion.ACTIVA);

        when(suscripcionService.crearSuscripcion(any(CrearSuscripcionRequest.class)))
                .thenReturn(suscripcionCreada);

        mockMvc.perform(post("/api/suscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void crearSuscripcion_conCupoCompleto_devuelve409ConMensaje() throws Exception {
        String mensaje = "Se alcanzó el cupo máximo (30) de la categoría 'Romance' para esta edición.";

        when(suscripcionService.crearSuscripcion(any(CrearSuscripcionRequest.class)))
                .thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(post("/api/suscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(mensaje));
    }
}
