package ar.edu.utn.cajasliterarias.backend;

import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PersistenciaIntegrationTest {

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private SuscriptorRepository suscriptorRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void suscripcionPruebaGuardarRecuperar() {
        Suscriptor suscriptor = new Suscriptor();
        suscriptor.setNombre("Aldana");
        suscriptor.setEmail("aldana@test.com");
        suscriptorRepository.save(suscriptor);

        Categoria categoria = new Categoria();
        categoria.setNombre("Narrativa/Drama");
        categoriaRepository.save(categoria);

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcionRepository.save(suscripcion);

        assertThat(suscripcionRepository.findAll()).hasSize(1);
    }
}
