package ar.edu.utn.cajasliterarias.backend.security;

import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.repository.SuscripcionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionAccesoTest {

    @Mock private SuscripcionRepository repo;
    @InjectMocks private SuscripcionAcceso acceso;

    private Suscripcion de(String email) {
        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setEmail(email);
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);
        return suscripcion;
    }

    @Test
    void propia_esTrue() {
        when(repo.findById(1L)).thenReturn(Optional.of(de("juana@mail.com")));
        assertThat(acceso.esPropia(1L, "juana@mail.com")).isTrue();
    }

    @Test
    void ignoraMayusculas() {
        when(repo.findById(1L)).thenReturn(Optional.of(de("juana@mail.com")));
        assertThat(acceso.esPropia(1L, "Juana@Mail.com")).isTrue();
    }

    @Test
    void ajena_esFalse() {
        when(repo.findById(1L)).thenReturn(Optional.of(de("otra@mail.com")));
        assertThat(acceso.esPropia(1L, "juana@mail.com")).isFalse();
    }

    @Test
    void inexistente_esFalse() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThat(acceso.esPropia(99L, "juana@mail.com")).isFalse();
    }

    @Test
    void idNulo_esFalseSinConsultarLaBase() {
        assertThat(acceso.esPropia(null, "juana@mail.com")).isFalse();
        verifyNoInteractions(repo);
    }
}
