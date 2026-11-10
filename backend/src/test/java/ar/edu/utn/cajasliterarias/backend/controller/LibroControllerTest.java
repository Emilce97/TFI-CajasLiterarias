package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.config.SecurityConfig;
import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.LibroService;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibroController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    @MockitoBean
    private JwtService jwtService;

    private Libro libro(Long id, String titulo) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setTitulo(titulo);
        return libro;
    }

    @Test
    void listar_comoAdmin_devuelve200ConLosLibros() throws Exception {
        when(libroService.listarLibros())
                .thenReturn(List.of(libro(1L, "El Aleph"), libro(2L, "Orgullo y prejuicio")));

        mockMvc.perform(get("/api/libros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].titulo").value("El Aleph"));
    }

    @Test
    @WithMockUser(roles = "SUSCRIPTOR")
    void listar_comoSuscriptor_devuelve403() throws Exception {
        mockMvc.perform(get("/api/libros"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(libroService);
    }

    @Test
    @WithAnonymousUser
    void listar_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/libros"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_creaUnLibro_devuelve201() throws Exception {
        Libro libro = new Libro();
        libro.setId(10L);
        libro.setTitulo("Rayuela");
        when(libroService.crearLibro(any())).thenReturn(libro);

        mockMvc.perform(post("/api/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Rayuela\",\"autor\":\"Julio Cortázar\",\"proveedorId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Rayuela"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void proveedorInexistente_devuelve400ConElMensaje() throws Exception {
        when(libroService.crearLibro(any()))
                .thenThrow(new IllegalArgumentException("No existe el proveedor con id 99."));

        mockMvc.perform(post("/api/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Rayuela\",\"proveedorId\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No existe el proveedor con id 99."));
    }

    @Test
    @WithMockUser(roles = "SUSCRIPTOR")
    void suscriptor_noPuedeCrearLibros() throws Exception {
        mockMvc.perform(post("/api/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Rayuela\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(libroService);
    }
}