package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarCategoriaRequest;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
public class SuscripcionService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private final SuscripcionRepository suscripcionRepository;
    private final SuscriptorRepository suscriptorRepository;
    private final CategoriaRepository categoriaRepository;
    private final EdicionRepository edicionRepository;
    private final CuraduriaEdicionRepository curaduriaEdicionRepository;
    private final Clock clock;

    public SuscripcionService(
            SuscripcionRepository suscripcionRepository,
            SuscriptorRepository suscriptorRepository,
            CategoriaRepository categoriaRepository,
            EdicionRepository edicionRepository,
            CuraduriaEdicionRepository curaduriaEdicionRepository,
            Clock clock
    ) {
        this.suscripcionRepository = suscripcionRepository;
        this.suscriptorRepository = suscriptorRepository;
        this.categoriaRepository = categoriaRepository;
        this.edicionRepository = edicionRepository;
        this.curaduriaEdicionRepository = curaduriaEdicionRepository;
        this.clock = clock;
    }

    /**
     * Da de alta una suscripcion a una categoria. No asume un Suscriptor ya registrado: busca por email
     * y, si no existe, lo crea con los datos del request (find-or-create). Si el email ya existe,
     * reutiliza ese Suscriptor tal cual esta en la base.
     * Valida que la categoria tenga curaduria en la edicion actualmente abierta y que no haya superado el cupoMaximo
     * definido para esa categoria/edicion.
     */
    @Transactional
    public Suscripcion crearSuscripcion(CrearSuscripcionRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email del suscriptor es obligatorio.");
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalArgumentException("El email tiene un formato inválido.");
        }
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del suscriptor es obligatorio.");
        }
        if (request.getCategoriaId() == null) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }

        Suscriptor suscriptor = suscriptorRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    Suscriptor nuevo = new Suscriptor();
                    nuevo.setNombre(request.getNombre());
                    nuevo.setEmail(request.getEmail());
                    nuevo.setDireccion(request.getDireccion());
                    nuevo.setFechaRegistro(LocalDate.now(clock));
                    return suscriptorRepository.save(nuevo);
                });

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
        suscripcion.setFechaAlta(LocalDate.now(clock));

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
        suscripcion.setFechaBaja(LocalDate.now(clock));
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

    /**
     * Cambia la categoría de una suscripción activa según la fecha de corte:
     * antes del corte o sin edición abierta, actualiza {@code categoria} y reemplaza
     * cambios pendientes; desde el corte, registra {@code proximaCategoria} y
     * {@code fechaSolicitudCambio} para la siguiente edición. Sin edición abierta,
     * no afecta la edición anterior ya congelada. El cupo se valida al ejecutar el corte.
     */
    @Transactional
    public Suscripcion cambiarCategoria(Long suscripcionId, CambiarCategoriaRequest request) {

        Suscripcion suscripcion = suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la suscripción con id " + suscripcionId
                ));

        if (suscripcion.getEstado() != EstadoSuscripcion.ACTIVA) {
            throw new IllegalStateException(
                    "Solo se puede cambiar la categoría de una suscripción ACTIVA (estado actual: "
                            + suscripcion.getEstado() + ")."
            );
        }

        Categoria categoriaNueva = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la categoría con id " + request.getCategoriaId()
                ));

        if (categoriaNueva.getId().equals(suscripcion.getCategoria().getId())) {
            throw new IllegalArgumentException(
                    "La suscripción ya pertenece a la categoria '" + categoriaNueva.getNombre() + "'."
            );
        }

        // El cupo de la categoría destino no se valida acá: se controla en CierreEdicionService, al ejecutar el corte.
        Edicion edicionAbierta = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        LocalDate hoy = LocalDate.now(clock);

        boolean desdeElCorte = edicionAbierta != null
                && edicionAbierta.getFechaCorte() != null
                && !hoy.isBefore(edicionAbierta.getFechaCorte());

        if (desdeElCorte) {
            // Desde el día del corte y hasta que se ejecute: rige desde la edición siguiente.
            suscripcion.setProximaCategoria(categoriaNueva);
            suscripcion.setFechaSolicitudCambio(hoy);
        } else {
            // Antes del corte, o sin edición abierta (la anterior ya está congelada): rige para la próxima edición que se cierre.
            suscripcion.setCategoria(categoriaNueva);
            suscripcion.setProximaCategoria(null);
            suscripcion.setFechaSolicitudCambio(null);
        }
        return suscripcionRepository.save(suscripcion);
    }
}
