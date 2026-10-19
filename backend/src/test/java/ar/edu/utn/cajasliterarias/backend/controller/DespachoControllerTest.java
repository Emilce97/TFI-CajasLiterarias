package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.config.SecurityConfig;
import ar.edu.utn.cajasliterarias.backend.dto.CambiarEstadoPedidoRequest;
import ar.edu.utn.cajasliterarias.backend.dto.PedidoDespachoResponse;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPedido;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.DespachoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la "puerta de entrada" HTTP del panel de despacho.
 * Las reglas del circuito las prueba DespachoServiceTest; aca se prueba que cada
 * endpoint responda con el codigo y el JSON correctos, y que solo entre la ADMIN.
 */
@WebMvcTest(DespachoController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = DespachoControllerTest.EMAIL, roles = "ADMIN")
class DespachoControllerTest {

    static final String EMAIL = "admin@cajasliterarias.com";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DespachoService despachoService;

    @MockitoBean
    private JwtService jwtService;

    // helpers

    private PedidoDespachoResponse pedido(EstadoPedido estado, String tracking) {
        return new PedidoDespachoResponse(7L, "Ana Pérez", "Calle Falsa 123", "Romance",
                "Orgullo y prejuicio", estado, tracking, null, null, null);
    }

    private String json(EstadoPedido estado, String tracking) throws Exception {
        return objectMapper.writeValueAsString(new CambiarEstadoPedidoRequest(estado, tracking, null));
    }

    // GET /api/ediciones/{id}/pedidos

    @Test
    void listar_edicionCerrada_devuelve200ConElDtoYSinLasRelaciones() throws Exception {
        when(despachoService.listarPedidos(1L, null))
                .thenReturn(List.of(pedido(EstadoPedido.PENDIENTE_DE_EMPAQUE, null)));

        mockMvc.perform(get("/api/ediciones/1/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreDestinatario").value("Ana Pérez"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE_DE_EMPAQUE"))
                // Es un DTO: no viajan la suscripcion, el pago ni la administradora.
                .andExpect(jsonPath("$[0].suscripcion").doesNotExist())
                .andExpect(jsonPath("$[0].pago").doesNotExist());
    }

    @Test
    void listar_conFiltro_pasaElEstadoAlService() throws Exception {
        when(despachoService.listarPedidos(1L, EstadoPedido.EMPAQUETADO))
                .thenReturn(List.of(pedido(EstadoPedido.EMPAQUETADO, null)));

        mockMvc.perform(get("/api/ediciones/1/pedidos").param("estado", "EMPAQUETADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("EMPAQUETADO"));
    }

    @Test
    void listar_edicionAbierta_devuelve409ConMensaje() throws Exception {
        String mensaje = "La edición todavía está abierta: los pedidos se generan en el corte";
        when(despachoService.listarPedidos(3L, null)).thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(get("/api/ediciones/3/pedidos"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Operación inválida"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    // PATCH /api/pedidos/{id}/estado

    @Test
    void cambiar_valido_devuelve200YLePasaAlServiceElEmailDeLaSesion() throws Exception {
        when(despachoService.cambiarEstado(eq(7L), any(CambiarEstadoPedidoRequest.class), eq(EMAIL)))
                .thenReturn(pedido(EstadoPedido.DESPACHADO, "AR123456"));

        mockMvc.perform(patch("/api/pedidos/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(EstadoPedido.DESPACHADO, "AR123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESPACHADO"))
                .andExpect(jsonPath("$.tracking").value("AR123456"));

        verify(despachoService).cambiarEstado(eq(7L), any(CambiarEstadoPedidoRequest.class), eq(EMAIL));
    }

    @Test
    void cambiar_salteandoUnPaso_devuelve409ConMensaje() throws Exception {
        String mensaje = "El pedido está PENDIENTE_DE_EMPAQUE: solo puede pasar a EMPAQUETADO";
        when(despachoService.cambiarEstado(eq(7L), any(CambiarEstadoPedidoRequest.class), eq(EMAIL)))
                .thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(patch("/api/pedidos/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(EstadoPedido.DESPACHADO, "AR123456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    void cambiar_despacharSinTracking_devuelve400ConMensaje() throws Exception {
        String mensaje = "Para despachar hace falta el número de tracking";
        when(despachoService.cambiarEstado(eq(7L), any(CambiarEstadoPedidoRequest.class), eq(EMAIL)))
                .thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(patch("/api/pedidos/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(EstadoPedido.DESPACHADO, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos inválidos"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    // seguridad

    @Test
    @WithAnonymousUser
    void listar_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/ediciones/1/pedidos"))
                .andExpect(status().isUnauthorized());

        verify(despachoService, never()).listarPedidos(any(), any());
    }

    @Test
    @WithAnonymousUser
    void cambiar_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(patch("/api/pedidos/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(EstadoPedido.EMPAQUETADO, null)))
                .andExpect(status().isUnauthorized());

        verify(despachoService, never()).cambiarEstado(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "SUSCRIPTOR")
    void listar_conOtroRol_devuelve403() throws Exception {
        // Las rutas del panel no estan declaradas: caen en anyRequest().hasRole("ADMIN").
        mockMvc.perform(get("/api/ediciones/1/pedidos"))
                .andExpect(status().isForbidden());

        verify(despachoService, never()).listarPedidos(any(), any());
    }
}