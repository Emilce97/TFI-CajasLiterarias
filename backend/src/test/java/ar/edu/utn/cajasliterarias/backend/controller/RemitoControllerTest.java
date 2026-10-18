package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.config.SecurityConfig;
import ar.edu.utn.cajasliterarias.backend.dto.RemitoResponse;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.RemitoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RemitoController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class RemitoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RemitoService remitoService;

    @MockitoBean
    private JwtService jwtService;

    private RemitoResponse remito() {
        RemitoResponse remito = new RemitoResponse();
        remito.setNumero(43L);
        remito.setFechaEmision(LocalDate.of(2026, 10, 17));
        remito.setEdicion("Edicion Octubre");
        remito.setNombreDestinatario("Nombre Original");
        remito.setDireccionEntrega("Calle Falsa 123");
        remito.setCategoria("Romance");
        remito.setLibro("Libro de prueba - Romance");
        remito.setEstado(EstadoPedido.EMPAQUETADO);
        return remito;
    }

    @Test
    void generarRemito_pedidoEmpaquetado_devuelve200ConLosDatos() throws Exception {
        when(remitoService.generarRemito(43L)).thenReturn(remito());

        mockMvc.perform(get("/api/pedidos/43/remito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(43))
                .andExpect(jsonPath("$.fechaEmision").value("2026-10-17"))
                .andExpect(jsonPath("$.nombreDestinatario").value("Nombre Original"))
                .andExpect(jsonPath("$.direccionEntrega").value("Calle Falsa 123"))
                .andExpect(jsonPath("$.libro").value("Libro de prueba - Romance"))
                .andExpect(jsonPath("$.estado").value("EMPAQUETADO"));
    }

    @Test
    void generarRemito_pedidoSinEmpaquetar_devuelve409() throws Exception {
        String mensaje = "El pedido 1 todavia no esta empaquetado.";
        when(remitoService.generarRemito(1L)).thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(get("/api/pedidos/1/remito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    void generarRemito_pedidoInexistente_devuelve400() throws Exception {
        String mensaje = "No existe el pedido con id 99";
        when(remitoService.generarRemito(99L)).thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(get("/api/pedidos/99/remito"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    @WithAnonymousUser
    void generarRemito_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/pedidos/43/remito"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUSCRIPTOR")
    void generarRemito_conOtroRol_devuelve403() throws Exception {
        mockMvc.perform(get("/api/pedidos/43/remito"))
                .andExpect(status().isForbidden());
    }
}
