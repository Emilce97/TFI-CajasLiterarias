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
     * y que no haya superado el cupoMaximo definido para esa categoria/edicion.
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
                    "El suscriptor ya tiene una suscripción activa en esta categoría."
            );
        }

        Edicion edicionAbierta = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        if (edicionAbierta == null) {
            throw new IllegalStateException(
                    "No hay ninguna edición abierta actualmente para dar de alta suscripciones."
            );
        }

        CuraduriaEdicion curaduria = curaduriaEdicionRepository
                .findByEdicionIdAndCategoriaId(edicionAbierta.getId(), categoria.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La categoría '" + categoria.getNombre()
                                + "' no tiene curaduria cargada en la edición abierta."
                ));

        long activasEnCategoria = suscripcionRepository
                .countByCategoriaIdAndEstado(categoria.getId(), EstadoSuscripcion.ACTIVA);

        if (curaduria.getCupoMaximo() != null && activasEnCategoria >= curaduria.getCupoMaximo()) {
            throw new IllegalStateException(
                    "Se alcanzó el cupo máximo (" + curaduria.getCupoMaximo()
                            + ") de la categoría '" + categoria.getNombre() + "' para esta edición."
            );
        }

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setSuscriptor(suscriptor);
        suscripcion.setCategoria(categoria);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcion.setFechaAlta(LocalDate.now());

        return suscripcionRepository.save(suscripcion);
    }

    /**
     * Da de baja definitivamente una suscripción. Cancela cualquier
     * cambio de categoria que hubiera quedado pendiente para el próximo corte.
     */
    @Transactional
    public void darDeBaja(Long suscripcionId) {
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la suscripcion con id " + suscripcionId
                ));

        if (suscripcion.getEstado() == EstadoSuscripcion.BAJA) {
            throw new IllegalStateException("La suscripción ya esta dada de baja.");
        }

        suscripcion.setEstado(EstadoSuscripcion.BAJA);
        suscripcion.setFechaBaja(LocalDate.now());
        suscripcion.setProximaCategoria(null);
        suscripcion.setFechaSolicitudCambio(null);

        suscripcionRepository.save(suscripcion);
    }

    /**
     * Pausa una suscripción actualmente activa. Una suscripción pausada
     * no ingresa al padrón del próximo corte hasta que se reanude.
     */
    @Transactional
    public void pausar(Long suscripcionId) {
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la suscripcion con id " + suscripcionId
                ));

        if (suscripcion.getEstado() != EstadoSuscripcion.ACTIVA) {
            throw new IllegalStateException(
                    "Solo se puede pausar una suscripción que este ACTIVA (estado actual: "
                            + suscripcion.getEstado() + ")."
            );
        }

        suscripcion.setEstado(EstadoSuscripcion.PAUSADA);
        suscripcionRepository.save(suscripcion);
    }

    /**
     * Reanuda una suscripción pausada, volviendo a dejarla ACTIVA.
     */
    @Transactional
    public void reanudar(Long suscripcionId) {
        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la suscripción con id " + suscripcionId
                ));

        if (suscripcion.getEstado() != EstadoSuscripcion.PAUSADA) {
            throw new IllegalStateException(
                    "Solo se puede reanudar una suscripción que esta PAUSADA (estado actual: "
                            + suscripcion.getEstado() + ")."
            );
        }

        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcionRepository.save(suscripcion);
    }
}
