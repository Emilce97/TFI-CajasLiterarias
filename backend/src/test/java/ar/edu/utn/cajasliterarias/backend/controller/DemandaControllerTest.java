package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.DemandaItemDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoFaltante;
import ar.edu.utn.cajasliterarias.backend.service.DemandaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemandaController.class)
class DemandaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DemandaService demandaService;

    private DemandaItemDTO item() {
        DemandaItemDTO item = new DemandaItemDTO();
        item.setId(5L);
        item.setLibroId(50L);
        item.setTitulo("Rayuela");
        item.setAutor("Julio Cortazar");
        item.setProveedorId(1L);
        item.setProveedorNombre("Libreria Sur");
        item.setCantidadRequerida(12);
        item.setCantidadRecibida(4);
        item.setCantidadFaltante(8);
        item.setEstadoFaltante(EstadoFaltante.FALTANTE);
        return item;
    }

    @Test
    void listarDemanda_edicionCerrada_devuelve200ConLasFilas() throws Exception {
        when(demandaService.listarDemandaDeEdicion(1L)).thenReturn(List.of(item()));

        mockMvc.perform(get("/api/ediciones/1/demanda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Rayuela"))
                .andExpect(jsonPath("$[0].proveedorNombre").value("Libreria Sur"))
                .andExpect(jsonPath("$[0].cantidadRequerida").value(12))
                .andExpect(jsonPath("$[0].cantidadRecibida").value(4))
                .andExpect(jsonPath("$[0].cantidadFaltante").value(8))
                .andExpect(jsonPath("$[0].estadoFaltante").value("FALTANTE"));
    }

    @Test
    void listarDemanda_edicionAbierta_devuelve409() throws Exception {
        String mensaje = "La edicion 3 sigue abierta: la demanda se calcula al cerrarla.";
        when(demandaService.listarDemandaDeEdicion(3L)).thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(get("/api/ediciones/3/demanda"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    void listarDemanda_edicionInexistente_devuelve400() throws Exception {
        String mensaje = "No existe la edicion con id 99";
        when(demandaService.listarDemandaDeEdicion(99L)).thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(get("/api/ediciones/99/demanda"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    // PATCH /api/demanda/{id}/recepcion

    @Test
    void registrarRecepcion_valida_devuelve200ConLaFilaActualizada() throws Exception {
        when(demandaService.registrarRecepcion(5L, 4)).thenReturn(item());

        mockMvc.perform(patch("/api/demanda/5/recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadRecibida\": 4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.cantidadRecibida").value(4))
                .andExpect(jsonPath("$.cantidadFaltante").value(8))
                .andExpect(jsonPath("$.estadoFaltante").value("FALTANTE"));
    }

    @Test
    void registrarRecepcion_cantidadNegativa_devuelve400() throws Exception {
        String mensaje = "La cantidad recibida es obligatoria y no puede ser negativa.";
        when(demandaService.registrarRecepcion(5L, -2)).thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(patch("/api/demanda/5/recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadRecibida\": -2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(mensaje));
    }
}