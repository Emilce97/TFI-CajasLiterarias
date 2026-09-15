package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.model.Categoria;
import ar.edu.utn.cajasliterarias.backend.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriaController.class)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaRepository categoriaRepository;

    @Test
    void listarCategorias_devuelveLasCuatroCategorias() throws Exception {
        Categoria misterio = new Categoria();
        misterio.setId(1L);
        misterio.setNombre("Misterio/Terror");

        Categoria romance = new Categoria();
        romance.setId(2L);
        romance.setNombre("Romance");

        when(categoriaRepository.findAll()).thenReturn(List.of(misterio, romance));

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Misterio/Terror"))
                .andExpect(jsonPath("$[1].nombre").value("Romance"));
    }
}