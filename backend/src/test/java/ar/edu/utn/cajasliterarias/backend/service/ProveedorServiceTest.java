package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearProveedorRequest;
import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import ar.edu.utn.cajasliterarias.backend.repository.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private ProveedorService proveedorService;

    @Test
    void crear_conDatosValidos_guardaSinEspaciosDeMas() {
        when(proveedorRepository.existsByNombreIgnoreCase("Editorial X")).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creado = proveedorService.crearProveedor(
                new CrearProveedorRequest("  Editorial X  ", " ventas@x.com "));

        assertThat(creado.getNombre()).isEqualTo("Editorial X");
        assertThat(creado.getContacto()).isEqualTo("ventas@x.com");
    }

    @Test
    void crear_conContactoVacio_loGuardaComoNull() {
        when(proveedorRepository.existsByNombreIgnoreCase("Editorial X")).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creado = proveedorService.crearProveedor(new CrearProveedorRequest("Editorial X", "   "));

        assertThat(creado.getContacto()).isNull();
    }

    @Test
    void crear_sinNombre_rechazaYNoConsultaLaBase() {
        assertThatThrownBy(() -> proveedorService.crearProveedor(new CrearProveedorRequest("   ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");

        verifyNoInteractions(proveedorRepository);
    }

    @Test
    void crear_conNombreDemasiadoLargo_rechaza() {
        String largo = "a".repeat(151);

        assertThatThrownBy(() -> proveedorService.crearProveedor(new CrearProveedorRequest(largo, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("150");

        verifyNoInteractions(proveedorRepository);
    }

    @Test
    void crear_conNombreRepetido_rechazaYNoGuarda() {
        when(proveedorRepository.existsByNombreIgnoreCase("Editorial X")).thenReturn(true);

        assertThatThrownBy(() -> proveedorService.crearProveedor(new CrearProveedorRequest("Editorial X", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");

        verify(proveedorRepository, never()).save(any());
    }
}