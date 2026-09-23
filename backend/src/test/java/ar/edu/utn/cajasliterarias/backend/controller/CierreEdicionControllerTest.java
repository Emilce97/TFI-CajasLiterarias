package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.ResumenCierreDTO;
import ar.edu.utn.cajasliterarias.backend.exception.EdicionYaCerradaException;
import ar.edu.utn.cajasliterarias.backend.service.CierreEdicionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CierreEdicionController.class)
class CierreEdicionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CierreEdicionService cierreEdicionService;

    @Test
    void cerrarEdicion_exitoso_devuelve200ConResumen() throws Exception {
        ResumenCierreDTO resumen = new ResumenCierreDTO();
        resumen.setEdicionId(1L);
        resumen.setTotalPedidosGenerados(3);
        resumen.setTotalExcluidos(1);
        resumen.setSuscripcionesExcluidas(List.of(99L));

        when(cierreEdicionService.ejecutarCorte()).thenReturn(resumen);

        mockMvc.perform(post("/api/ediciones/cierre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edicionId").value(1))
                .andExpect(jsonPath("$.totalPedidosGenerados").value(3))
                .andExpect(jsonPath("$.totalExcluidos").value(1))
                .andExpect(jsonPath("$.suscripcionesExcluidas[0]").value(99));
    }

    @Test
    void cerrarEdicion_edicionYaCerrada_devuelve409ConMensaje() throws Exception {
        when(cierreEdicionService.ejecutarCorte())
                .thenThrow(new EdicionYaCerradaException(1L));

        mockMvc.perform(post("/api/ediciones/cierre"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Edición ya cerrada"))
                .andExpect(jsonPath("$.message").value(
                        "La edicion con id 1 ya fue cerrada; no puede reprocesarse el corte."));
    }

    @Test
    void cerrarEdicion_sinEdicionAbierta_devuelve409ConMensaje() throws Exception {
        String mensaje = "No hay ninguna edición abierta para cerrar.";

        when(cierreEdicionService.ejecutarCorte())
                .thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(post("/api/ediciones/cierre"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Operación inválida"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }
}