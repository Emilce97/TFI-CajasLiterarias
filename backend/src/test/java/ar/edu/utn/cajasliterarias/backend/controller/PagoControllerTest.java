package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.dto.CrearPagoRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoPago;
import ar.edu.utn.cajasliterarias.backend.model.Pago;
import ar.edu.utn.cajasliterarias.backend.service.PagoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la "puerta de entrada" HTTP de pagos.
 * El service esta simulado: aca no se prueban las reglas (eso lo hace PagoServiceTest),
 * sino que cada endpoint responda en la ruta correcta, con el codigo HTTP correcto
 * y con el JSON bien armado, incluidos los errores que traduce GlobalExceptionHandler.
 */
@WebMvcTest(PagoController.class)
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PagoService pagoService;

    // helpers

    private CrearPagoRequest requestValido() {
        CrearPagoRequest request = new CrearPagoRequest();
        request.setSuscripcionId(10L);
        request.setEdicionId(1L);
        request.setMonto(new BigDecimal("5000.00"));
        return request;
    }

    private Pago pago(Long id, EstadoPago estado) {
        Pago pago = new Pago();
        pago.setId(id);
        pago.setMonto(new BigDecimal("5000.00"));
        pago.setEstado(estado);
        return pago;
    }

    // POST /api/pagos

    @Test
    void registrar_conDatosValidos_devuelve201ConPagoPendiente() throws Exception {
        when(pagoService.registrarPago(any(CrearPagoRequest.class)))
                .thenReturn(pago(5L, EstadoPago.PENDIENTE));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.monto").value(5000.00));
    }

    @Test
    void registrar_conMontoInvalido_devuelve400ConMensaje() throws Exception {
        String mensaje = "El monto del pago es obligatorio y debe ser mayor a cero.";
        when(pagoService.registrarPago(any(CrearPagoRequest.class)))
                .thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos inválidos"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    void registrar_duplicado_devuelve409ConMensaje() throws Exception {
        String mensaje = "Ya existe un pago registrado para esta suscripcion en esta edicion.";
        when(pagoService.registrarPago(any(CrearPagoRequest.class)))
                .thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Operación inválida"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    // GET /api/pagos/pendientes

    @Test
    void listarPendientes_devuelve200ConLaLista() throws Exception {
        when(pagoService.listarPendientes())
                .thenReturn(List.of(pago(5L, EstadoPago.PENDIENTE), pago(6L, EstadoPago.PENDIENTE)));

        mockMvc.perform(get("/api/pagos/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[1].id").value(6))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void listarPendientes_sinPendientes_devuelve200ConListaVacia() throws Exception {
        when(pagoService.listarPendientes()).thenReturn(List.of());

        mockMvc.perform(get("/api/pagos/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // PATCH /api/pagos/{id}/validar

    @Test
    void validar_pagoPendiente_devuelve200ConPagoValidado() throws Exception {
        Pago validado = pago(5L, EstadoPago.VALIDADO);
        validado.setFechaValidacion(LocalDateTime.of(2026, 10, 10, 12, 0));
        when(pagoService.validarPago(5L)).thenReturn(validado);

        mockMvc.perform(patch("/api/pagos/5/validar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.fechaValidacion").exists());
    }

    @Test
    void validar_pagoYaValidado_devuelve409ConMensaje() throws Exception {
        String mensaje = "El pago 5 ya fue validado el 2026-10-01T09:30.";
        when(pagoService.validarPago(5L)).thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(patch("/api/pagos/5/validar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Operación inválida"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    void validar_pagoInexistente_devuelve400ConMensaje() throws Exception {
        String mensaje = "No existe el pago con id 99";
        when(pagoService.validarPago(99L)).thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(patch("/api/pagos/99/validar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos inválidos"))
                .andExpect(jsonPath("$.message").value(mensaje));
    }
}