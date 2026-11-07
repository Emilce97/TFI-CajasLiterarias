package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CambiarCategoriaRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CrearSuscripcionRequest;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final SuscriptorRepository suscriptorRepository;
    private final CategoriaRepository categoriaRepository;
    private final EdicionRepository edicionRepository;
    private final CuraduriaEdicionRepository curaduriaEdicionRepository;
    private final PedidoEdicionRepository pedidoEdicionRepository;
    private final Clock clock;

    public SuscripcionService(
            SuscripcionRepository suscripcionRepository,
            SuscriptorRepository suscriptorRepository,
            CategoriaRepository categoriaRepository,
            EdicionRepository edicionRepository,
            CuraduriaEdicionRepository curaduriaEdicionRepository,
            PedidoEdicionRepository pedidoEdicionRepository,
            Clock clock
    ) {
        this.suscripcionRepository = suscripcionRepository;
        this.suscriptorRepository = suscriptorRepository;
        this.categoriaRepository = categoriaRepository;
        this.edicionRepository = edicionRepository;
        this.curaduriaEdicionRepository = curaduriaEdicionRepository;
        this.pedidoEdicionRepository = pedidoEdicionRepository;
        this.clock = clock;
    }

    /**
     * Da de alta una suscripcion a una categoria para el suscriptor logueado (se identifica por el email del token;
     * la cuenta se crea en el registro). Valida que la categoria tenga curaduria en la edicion actualmente abierta
     * y que no haya superado el cupoMaximo definido para esa categoria/edicion.
     */
    @Transactional
    public Suscripcion crearSuscripcion(String emailSuscriptor, CrearSuscripcionRequest request) {

        if (request == null || request.getCategoriaId() == null) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }

        Suscriptor suscriptor = suscriptorRepository.findByEmail(emailSuscriptor)
                .orElseThrow(() -> new IllegalArgumentException("No existe un suscriptor con ese email."));

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

        // Desde el día del corte la ventana de altas de esta edición está cerrada: una suscripción nueva
        // no puede entrar al padrón que se está por congelar, y el cupo de la edición siguiente todavía
        // no se puede validar porque esa edición no existe.
        if (esDesdeElCorte(edicionAbierta, LocalDate.now(clock))) {
            throw new IllegalStateException(
                    "Las altas de la edición '" + edicionAbierta.getNombre() + "' cerraron el "
                            + edicionAbierta.getFechaCorte()
                            + ". Podés suscribirte cuando se abra la próxima edición."
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

    @Transactional(readOnly = true)
    public List<Suscripcion> listarDelSuscriptor(String emailSuscriptor) {
        return suscripcionRepository.findBySuscriptorEmail(emailSuscriptor);
    }

    /**
     * Da de baja una suscripción según la fecha de corte: antes del corte (o sin edición abierta) la baja es inmediata
     * y cancela cualquier cambio de categoria pendiente; desde el día del corte queda pendiente y se aplica
     * al ejecutar el cierre, así la suscripción sigue contando en la edición que se cierra y la baja rige desde la siguiente.
     */
    @Transactional
    public void darDeBaja(Long suscripcionId) {
        Suscripcion suscripcion = buscarSuscripcion(suscripcionId);

        if (estadoEfectivo(suscripcion) == EstadoSuscripcion.BAJA) {
            throw new IllegalStateException("La suscripción ya está dada de baja o tiene una baja pendiente.");
        }

        solicitarEstado(suscripcion, EstadoSuscripcion.BAJA);
    }

    /**
     * Pausa una suscripción activa según la fecha de corte: antes del corte la pausa es inmediata
     * (no ingresa al padrón de la edición que se cierra); desde el día del corte queda pendiente y
     * la suscripción todavía entra a esta edición.
     */
    @Transactional
    public void pausar(Long suscripcionId) {
        Suscripcion suscripcion = buscarSuscripcion(suscripcionId);

        if (estadoEfectivo(suscripcion) != EstadoSuscripcion.ACTIVA) {
            throw new IllegalStateException(
                    "Solo se puede pausar una suscripción que esté ACTIVA (estado actual: "
                            + estadoEfectivo(suscripcion) + ")."
            );
        }

        solicitarEstado(suscripcion, EstadoSuscripcion.PAUSADA);
    }

    /**
     * Reanuda una suscripción pausada según la fecha de corte: antes del corte vuelve a ACTIVA
     * enseguida (puede entrar a la edición que se cierra); desde el día del corte queda pendiente
     * y rige desde la siguiente. Reanudar una suscripción con pausa pendiente cancela esa pausa.
     */
    @Transactional
    public void reanudar(Long suscripcionId) {
        Suscripcion suscripcion = buscarSuscripcion(suscripcionId);

        if (estadoEfectivo(suscripcion) != EstadoSuscripcion.PAUSADA) {
            throw new IllegalStateException(
                    "Solo se puede reanudar una suscripción que esté PAUSADA (estado actual: "
                            + estadoEfectivo(suscripcion) + ")."
            );
        }

        solicitarEstado(suscripcion, EstadoSuscripcion.ACTIVA);
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
        LocalDate hoy = LocalDate.now(clock);
        Edicion edicionAbierta = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);

        if (esDesdeElCorte(edicionAbierta, hoy)) {
            // Desde el día del corte y hasta que se ejecute: rige desde la edición siguiente.
            suscripcion.setProximaCategoria(categoriaNueva);
            suscripcion.setFechaSolicitudCambio(hoy);
        } else {
            // El cambio rige para la edición abierta: si ya recibió el libro de la temática nueva, se rechaza.
            // Desde el día del corte no se valida: el cambio rige desde la edición siguiente, que todavía no tiene
            // curaduría; ahí la repetición se detecta y se marca en el corte.
            validarLibroNoRecibido(suscripcion, categoriaNueva, edicionAbierta);
            // Antes del corte, o sin edición abierta (la anterior ya está congelada): rige para la próxima edición que se cierre.
            suscripcion.setCategoria(categoriaNueva);
            suscripcion.setProximaCategoria(null);
            suscripcion.setFechaSolicitudCambio(null);
        }
        return suscripcionRepository.save(suscripcion);
    }

    private Suscripcion buscarSuscripcion(Long suscripcionId) {
        return suscripcionRepository.findById(suscripcionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la suscripción con id " + suscripcionId
                ));
    }

    /**
     * No repetición de títulos: rechaza el cambio si el suscriptor ya recibió (en un pedido anterior)
     * el libro que la temática nueva tiene curado en la edición abierta. Sin edición abierta o sin curaduría
     * para esa temática no hay nada que comparar.
     */
    private void validarLibroNoRecibido(Suscripcion suscripcion, Categoria categoriaNueva, Edicion edicionAbierta) {
        if (edicionAbierta == null) {
            return;
        }
        curaduriaEdicionRepository
                .findByEdicionIdAndCategoriaId(edicionAbierta.getId(), categoriaNueva.getId())
                .ifPresent(curaduria -> {
                    Libro libro = curaduria.getLibro();
                    if (pedidoEdicionRepository.existsBySuscripcionSuscriptorIdAndCuraduriaLibroId(
                            suscripcion.getSuscriptor().getId(), libro.getId())) {
                        throw new IllegalStateException(
                                "Ya recibiste «" + libro.getTitulo() + "», el libro de "
                                        + categoriaNueva.getNombre() + " en esta edición. "
                                        + "Elegí otra temática para no repetir el título.");
                    }
                });
    }

    // Estado que va a tener la suscripción una vez aplicado lo pendiente.
    private EstadoSuscripcion estadoEfectivo(Suscripcion suscripcion) {
        return suscripcion.getProximoEstado() != null
                ? suscripcion.getProximoEstado()
                : suscripcion.getEstado();
    }

    // Desde el día del corte (inclusive) y hasta que se ejecute el cierre, los cambios rigen desde la edición siguiente.
    // Sin edición abierta (la anterior ya está congelada), el cambio es inmediato.
    private boolean esDesdeElCorte(LocalDate hoy) {
        return esDesdeElCorte(edicionRepository.findByEstado(EstadoEdicion.ABIERTA), hoy);
    }

    private boolean esDesdeElCorte(Edicion edicion, LocalDate hoy) {
        return edicion != null
                && edicion.getFechaCorte() != null
                && !hoy.isBefore(edicion.getFechaCorte());
    }

    /**
     * Antes del corte aplica el estado ya. Desde el corte lo deja como pendiente (proximoEstado + fechaSolicitudEstado),
     * salvo que el destino coincida con el estado vigente: en ese caso cancela el pedido pendiente anterior.
     */
    private void solicitarEstado(Suscripcion suscripcion, EstadoSuscripcion destino) {
        LocalDate hoy = LocalDate.now(clock);

        if (esDesdeElCorte(hoy)) {
            if (destino == suscripcion.getEstado()) {
                suscripcion.setProximoEstado(null);
                suscripcion.setFechaSolicitudEstado(null);
            } else {
                suscripcion.setProximoEstado(destino);
                suscripcion.setFechaSolicitudEstado(hoy);
            }
        } else {
            suscripcion.setProximoEstado(destino);
            suscripcion.setFechaSolicitudEstado(hoy);
            suscripcion.promoverProximoEstado();
        }
        suscripcionRepository.save(suscripcion);
    }
}
