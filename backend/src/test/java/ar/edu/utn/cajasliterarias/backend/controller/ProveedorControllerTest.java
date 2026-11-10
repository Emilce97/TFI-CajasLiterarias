package ar.edu.utn.cajasliterarias.backend.controller;

import ar.edu.utn.cajasliterarias.backend.config.SecurityConfig;
import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import ar.edu.utn.cajasliterarias.backend.service.ProveedorService;
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

@WebMvcTest(ProveedorController.class)
@Import(SecurityConfig.class)
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProveedorService proveedorService;

    @MockitoBean
    private JwtService jwtService;

    private static final String JSON_PROVEEDOR = "{\"nombre\":\"Editorial X\",\"contacto\":\"ventas@x.com\"}";

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_veLaListaDeProveedores() throws Exception {
        when(proveedorService.listarProveedores()).thenReturn(List.of(proveedor(1L, "Editorial X")));

        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Editorial X"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_creaUnProveedor_devuelve201() throws Exception {
        when(proveedorService.crearProveedor(any())).thenReturn(proveedor(5L, "Editorial X"));

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROVEEDOR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Editorial X"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void datosInvalidos_devuelve400ConElMensaje() throws Exception {
        when(proveedorService.crearProveedor(any()))
                .thenThrow(new IllegalArgumentException("El nombre del proveedor es obligatorio."));

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre del proveedor es obligatorio."));
    }

    @Test
    @WithMockUser(roles = "SUSCRIPTOR")
    void suscriptor_noPuedeVerProveedores() throws Exception {
        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(proveedorService);
    }

    @Test
    @WithMockUser(roles = "SUSCRIPTOR")
    void suscriptor_noPuedeCrearProveedores() throws Exception {
        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROVEEDOR))
                .andExpect(status().isForbidden());

        verifyNoInteractions(proveedorService);
    }

    @Test
    @WithAnonymousUser
    void sinLogin_devuelve401() throws Exception {
        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(proveedorService);
    }

    private Proveedor proveedor(Long id, String nombre) {
        Proveedor p = new Proveedor();
        p.setId(id);
        p.setNombre(nombre);
        return p;
    }
}