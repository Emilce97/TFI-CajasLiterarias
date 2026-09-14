package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearEdicionRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CuraduriaItemDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.service.EdicionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EdicionController.class)
class EdicionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private EdicionService edicionService;

    private CrearEdicionRequest requestValido() {
        CuraduriaItemDTO item = new CuraduriaItemDTO();
        item.setCategoriaId(1L);
        item.setLibroId(1L);
        item.setPrecioVigente(new BigDecimal("5000"));
        item.setCupoMaximo(30);

        CrearEdicionRequest request = new CrearEdicionRequest();
        request.setNombre("Octubre 2026");
        request.setFechaCorte(LocalDate.of(2026, 10, 21));
        request.setFechaDespachoDesde(LocalDate.of(2026, 11, 1));
        request.setFechaDespachoHasta(LocalDate.of(2026, 11, 5));
        request.setCuradurias(List.of(item, item, item, item));
        return request;
    }

    @Test
    void crearEdicion_conDatosValidos_devuelve201() throws Exception {
        Edicion edicionCreada = new Edicion();
        edicionCreada.setId(1L);
        edicionCreada.setNombre("Octubre 2026");
        edicionCreada.setEstado(EstadoEdicion.ABIERTA);

        when(edicionService.crearEdicion(any(CrearEdicionRequest.class)))
                .thenReturn(edicionCreada);

        mockMvc.perform(post("/api/ediciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Octubre 2026"))
                .andExpect(jsonPath("$.estado").value("ABIERTA"));
    }

    @Test
    void crearEdicion_conNombreDuplicado_devuelve400() throws Exception {
        when(edicionService.crearEdicion(any(CrearEdicionRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Ya existe una edición con el nombre 'Octubre 2026'."
                ));

        mockMvc.perform(post("/api/ediciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadRequest());
    }
}