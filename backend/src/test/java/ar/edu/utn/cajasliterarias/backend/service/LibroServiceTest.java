package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearLibroRequest;
import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import ar.edu.utn.cajasliterarias.backend.repository.LibroRepository;
import ar.edu.utn.cajasliterarias.backend.repository.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @InjectMocks
    private LibroService libroService;

    @Test
    void crear_conProveedor_loAsociaYGuardaSinEspaciosDeMas() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Editorial X");
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(libroRepository.existsByTituloIgnoreCase("Rayuela")).thenReturn(false);
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        Libro creado = libroService.crearLibro(new CrearLibroRequest(" Rayuela ", " Julio Cortázar ", 1L));

        assertThat(creado.getTitulo()).isEqualTo("Rayuela");
        assertThat(creado.getAutor()).isEqualTo("Julio Cortázar");
        assertThat(creado.getProveedor()).isEqualTo(proveedor);
    }

    @Test
    void crear_sinProveedorNiAutor_guardaAmbosComoNull() {
        when(libroRepository.existsByTituloIgnoreCase("Rayuela")).thenReturn(false);
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        Libro creado = libroService.crearLibro(new CrearLibroRequest("Rayuela", "  ", null));

        assertThat(creado.getAutor()).isNull();
        assertThat(creado.getProveedor()).isNull();
        verifyNoInteractions(proveedorRepository);
    }

    @Test
    void crear_sinTitulo_rechazaYNoConsultaLaBase() {
        assertThatThrownBy(() -> libroService.crearLibro(new CrearLibroRequest("   ", "Autor", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");

        verifyNoInteractions(libroRepository, proveedorRepository);
    }

    @Test
    void crear_conProveedorInexistente_rechazaYNoGuarda() {
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.crearLibro(new CrearLibroRequest("Rayuela", null, 99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe el proveedor");

        verify(libroRepository, never()).save(any());
    }

    @Test
    void crear_conTituloRepetido_rechazaYNoGuarda() {
        when(libroRepository.existsByTituloIgnoreCase("Rayuela")).thenReturn(true);

        assertThatThrownBy(() -> libroService.crearLibro(new CrearLibroRequest("Rayuela", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");

        verify(libroRepository, never()).save(any());
    }
}