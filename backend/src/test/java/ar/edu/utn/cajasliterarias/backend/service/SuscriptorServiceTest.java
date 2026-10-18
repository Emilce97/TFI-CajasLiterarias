package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.ActualizarPerfilRequest;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.repository.SuscriptorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscriptorServiceTest {

    @Mock private SuscriptorRepository suscriptorRepository;
    private SuscriptorService service;

    @BeforeEach
    void setUp() {
        service = new SuscriptorService(suscriptorRepository);
    }

    private Suscriptor juana() {
        Suscriptor s = new Suscriptor();
        s.setId(7L);
        s.setEmail("juana@mail.com");
        s.setNombre("Juana Diaz");
        s.setDireccion("Calle Falsa 123");
        when(suscriptorRepository.findByEmail("juana@mail.com")).thenReturn(Optional.of(s));
        return s;
    }

    private ActualizarPerfilRequest perfil(String nombre, String direccion) {
        ActualizarPerfilRequest r = new ActualizarPerfilRequest();
        r.setNombre(nombre);
        r.setDireccion(direccion);
        return r;
    }

    @Test
    void actualizarSoloLaDireccion_conservaElNombre() {
        Suscriptor s = juana();
        when(suscriptorRepository.save(any(Suscriptor.class))).thenAnswer(i -> i.getArgument(0));

        service.actualizarPerfil("juana@mail.com", perfil(null, "  Av. Siempreviva 742 "));

        assertEquals("Av. Siempreviva 742", s.getDireccion());
        assertEquals("Juana Diaz", s.getNombre());
    }

    @Test
    void actualizarNombreYDireccion() {
        Suscriptor s = juana();
        when(suscriptorRepository.save(any(Suscriptor.class))).thenAnswer(i -> i.getArgument(0));

        service.actualizarPerfil("juana@mail.com", perfil("Juana Diaz de Perez", "Av. Siempreviva 742"));

        assertEquals("Juana Diaz de Perez", s.getNombre());
        assertEquals("Av. Siempreviva 742", s.getDireccion());
    }

    @Test
    void sinNingunCampo_lanzaExcepcionSinGuardar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarPerfil("juana@mail.com", perfil(null, null)));
        verify(suscriptorRepository, never()).save(any(Suscriptor.class));
    }

    @Test
    void campoEnBlanco_lanzaExcepcionYNoModifica() {
        Suscriptor s = juana();

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarPerfil("juana@mail.com", perfil(null, "   ")));
        assertEquals("Calle Falsa 123", s.getDireccion());
        verify(suscriptorRepository, never()).save(any(Suscriptor.class));
    }

    @Test
    void direccionDeMasDe255Caracteres_lanzaExcepcion() {
        juana();

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarPerfil("juana@mail.com", perfil(null, "x".repeat(256))));
    }

    @Test
    void suscriptorInexistente_lanzaExcepcion() {
        when(suscriptorRepository.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.obtenerPerfil("nadie@mail.com"));
    }
}
