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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ar.edu.utn.cajasliterarias.backend.model.Categoria;
import ar.edu.utn.cajasliterarias.backend.model.CuraduriaEdicion;
import ar.edu.utn.cajasliterarias.backend.model.Libro;

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
    @Test
    void listarEdiciones_devuelveLaListaDeEdiciones() throws Exception {
        Edicion edicion1 = new Edicion();
        edicion1.setId(1L);
        edicion1.setNombre("Septiembre 2026");
        edicion1.setEstado(EstadoEdicion.CERRADA);

        Edicion edicion2 = new Edicion();
        edicion2.setId(2L);
        edicion2.setNombre("Octubre 2026");
        edicion2.setEstado(EstadoEdicion.ABIERTA);

        when(edicionService.listarEdiciones()).thenReturn(List.of(edicion1, edicion2));

        mockMvc.perform(get("/api/ediciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Septiembre 2026"))
                .andExpect(jsonPath("$[1].nombre").value("Octubre 2026"));
    }
    @Test
    void listarCuradurias_conEdicionExistente_devuelveLista() throws Exception {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Romance");

        Libro libro = new Libro();
        libro.setId(1L);
        libro.setTitulo("El Aleph");

        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setId(1L);
        curaduria.setCategoria(categoria);
        curaduria.setLibro(libro);
        curaduria.setPrecioVigente(new BigDecimal("5000"));
        curaduria.setCupoMaximo(30);

        when(edicionService.listarCuraduriasDeEdicion(1L)).thenReturn(List.of(curaduria));

        mockMvc.perform(get("/api/ediciones/1/curadurias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria.nombre").value("Romance"))
                .andExpect(jsonPath("$[0].libro.titulo").value("El Aleph"))
                .andExpect(jsonPath("$[0].cupoMaximo").value(30));
    }

    @Test
    void listarCuradurias_conEdicionInexistente_devuelve404() throws Exception {
        when(edicionService.listarCuraduriasDeEdicion(99L))
                .thenThrow(new IllegalArgumentException("No existe la edicion con id 99"));

        mockMvc.perform(get("/api/ediciones/99/curadurias"))
                .andExpect(status().isNotFound());
    }
}