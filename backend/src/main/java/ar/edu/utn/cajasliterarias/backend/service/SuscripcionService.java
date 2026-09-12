package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearSuscripcionRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Categoria;
import ar.edu.utn.cajasliterarias.backend.model.CuraduriaEdicion;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.repository.CategoriaRepository;
import ar.edu.utn.cajasliterarias.backend.repository.CuraduriaEdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.SuscripcionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.SuscriptorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final SuscriptorRepository suscriptorRepository;
    private final CategoriaRepository categoriaRepository;
    private final EdicionRepository edicionRepository;
    private final CuraduriaEdicionRepository curaduriaEdicionRepository;

    public SuscripcionService(
            SuscripcionRepository suscripcionRepository,
            SuscriptorRepository suscriptorRepository,
            CategoriaRepository categoriaRepository,
            EdicionRepository edicionRepository,
            CuraduriaEdicionRepository curaduriaEdicionRepository
    ) {
        this.suscripcionRepository = suscripcionRepository;
        this.suscriptorRepository = suscriptorRepository;
        this.categoriaRepository = categoriaRepository;
        this.edicionRepository = edicionRepository;
        this.curaduriaEdicionRepository = curaduriaEdicionRepository;
    }

    /**
     * Da de alta una suscripcion de un suscriptor ya registrado a una categoria.
     * Valida que la categoria tenga curaduria en la edicion actualmente abierta
     * y que no haya superado el cupoMaximo definido para esa categoria/edicion
     * (cupo validado aca, no en el corte, segun lo acordado con el tutor).
     */
    @Transactional
    public Suscripcion crearSuscripcion(CrearSuscripcionRequest request) {

        Suscriptor suscriptor = suscriptorRepository.findById(request.getSuscriptorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el suscriptor con id " + request.getSuscriptorId()
                ));

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la categoria con id " + request.getCategoriaId()
                ));

        if (suscripcionRepository.existsBySuscriptorIdAndCategoriaIdAndEstado(
                suscriptor.getId(), categoria.getId(), EstadoSuscripcion.ACTIVA)) {
            throw new IllegalArgumentException(
                    "El suscriptor ya tiene una suscripcion activa en esta categoria."
            );
        }

        Edicion edicionAbierta = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        if (edicionAbierta == null) {
            throw new IllegalStateException(
                    "No hay ninguna edicion abierta actualmente para dar de alta suscripciones."
            );
        }

        CuraduriaEdicion curaduria = curaduriaEdicionRepository
                .findByEdicionIdAndCategoriaId(edicionAbierta.getId(), categoria.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La categoria '" + categoria.getNombre()
                                + "' no tiene curaduria cargada en la edicion abierta."
                ));

        long activasEnCategoria = suscripcionRepository
                .countByCategoriaIdAndEstado(categoria.getId(), EstadoSuscripcion.ACTIVA);

        if (curaduria.getCupoMaximo() != null && activasEnCategoria >= curaduria.getCupoMaximo()) {
            throw new IllegalStateException(
                    "Se alcanzo el cupo maximo (" + curaduria.getCupoMaximo()
                            + ") de la categoria '" + categoria.getNombre() + "' para esta edicion."
            );
        }

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaAlta(LocalDate.now());

        return suscripcionRepository.save(suscripcion);
    }
}
