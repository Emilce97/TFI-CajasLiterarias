package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearEdicionRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CuraduriaItemDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.model.*;
import ar.edu.utn.cajasliterarias.backend.repository.CategoriaRepository;
import ar.edu.utn.cajasliterarias.backend.repository.CuraduriaEdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.LibroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;

@Service
public class EdicionService {

    private final EdicionRepository edicionRepository;
    private final CuraduriaEdicionRepository curaduriaEdicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final LibroRepository libroRepository;

    public EdicionService(
            EdicionRepository edicionRepository,
            CuraduriaEdicionRepository curaduriaEdicionRepository,
            CategoriaRepository categoriaRepository,
            LibroRepository libroRepository
    ) {
        this.edicionRepository = edicionRepository;
        this.curaduriaEdicionRepository = curaduriaEdicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.libroRepository = libroRepository;
    }

    /**
     * Crea una edicion nueva junto con la curaduria de sus 4 categorias
     * (libro, precio y cupo maximo por cada una).
     * Datos incompletos o incoherentes -> IllegalArgumentException (400).
     * Ya hay una edicion ABIERTA -> IllegalStateException (409).
     */
    @Transactional
    public Edicion crearEdicion(CrearEdicionRequest request) {
        validarDatosGenerales(request);

        String nombre = request.getNombre().trim();
        if (edicionRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException(
                    "Ya existe una edicion con el nombre '" + nombre + "'."
            );
        }

        validarCuradurias(request.getCuradurias());

        // Solo puede haber una edicion ABIERTA al mismo tiempo: es una regla de estado (409).
        if (edicionRepository.findByEstado(EstadoEdicion.ABIERTA) != null) {
            throw new IllegalStateException(
                    "Ya existe una edicion ABIERTA. Debe cerrarse antes de crear una nueva."
            );
        }

        Edicion edicion = new Edicion();
        edicion.setNombre(nombre);
        edicion.setFechaCorte(request.getFechaCorte());
        edicion.setFechaDespachoDesde(request.getFechaDespachoDesde());
        edicion.setFechaDespachoHasta(request.getFechaDespachoHasta());
        edicion.setEstado(EstadoEdicion.ABIERTA);
        edicion = edicionRepository.save(edicion);

        for (CuraduriaItemDTO item : request.getCuradurias()) {
            crearCuraduria(edicion, item);
        }

        return edicion;
    }

    /** Nombre y fechas obligatorios, y el despacho siempre despues del corte. */
    private void validarDatosGenerales(CrearEdicionRequest request) {
        if (request == null || request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la edicion es obligatorio.");
        }
        LocalDate corte = request.getFechaCorte();
        LocalDate desde = request.getFechaDespachoDesde();
        LocalDate hasta = request.getFechaDespachoHasta();
        if (corte == null || desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "La fecha de corte y las fechas de despacho son obligatorias."
            );
        }
        if (!desde.isAfter(corte)) {
            throw new IllegalArgumentException(
                    "El despacho tiene que empezar despues de la fecha de corte."
            );
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException(
                    "La fecha 'despacho hasta' no puede ser anterior a 'despacho desde'."
            );
        }
    }

    /**
     * Exactamente 4 curadurias completas, de categorias distintas, con precio y cupo positivos.
     * Precio unico por edicion: las 4 tematicas cuestan lo mismo (simplificacion P0 documentada en der.md).
     */
    private void validarCuradurias(List<CuraduriaItemDTO> curadurias) {
        if (curadurias == null || curadurias.size() != 4) {
            throw new IllegalArgumentException(
                    "La edicion debe tener exactamente 4 curadurias (una por categoria)."
            );
        }

        for (CuraduriaItemDTO item : curadurias) {
            if (item == null || item.getCategoriaId() == null
                    || item.getLibroId() == null || item.getPrecioVigente() == null) {
                throw new IllegalArgumentException("Cada curaduria necesita categoria, libro y precio.");
            }
            if (item.getPrecioVigente().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio debe ser mayor a cero.");
            }
            if (item.getCupoMaximo() != null && item.getCupoMaximo() <= 0) {
                throw new IllegalArgumentException("El cupo maximo, si se indica, debe ser mayor a cero.");
            }
        }

        long categoriasDistintas = curadurias.stream()
                .map(CuraduriaItemDTO::getCategoriaId)
                .distinct()
                .count();
        if (categoriasDistintas != 4) {
            throw new IllegalArgumentException(
                    "Las 4 curadurias deben corresponder a categorias distintas."
            );
        }

        BigDecimal precio = curadurias.get(0).getPrecioVigente();
        boolean mismoPrecio = curadurias.stream()
                .allMatch(item -> item.getPrecioVigente().compareTo(precio) == 0);
        if (!mismoPrecio) {
            throw new IllegalArgumentException(
                    "El precio debe ser el mismo para las 4 tematicas (precio unico por edicion)."
            );
        }
    }

    /**
     * Lista las ediciones visibles para quien consulta.
     * Con incluirCerradas=true devuelve todas (administradora); con false, solo la abierta (público).
     */
    public List<Edicion> listarEdiciones(boolean incluirCerradas) {
        if (incluirCerradas) {
            return edicionRepository.findAll();
        }
        Edicion abierta = edicionRepository.findByEstado(EstadoEdicion.ABIERTA);
        return abierta == null ? List.of() : List.of(abierta);
    }

    private void crearCuraduria(Edicion edicion, CuraduriaItemDTO item) {
        Categoria categoria = categoriaRepository.findById(item.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la categoria con id " + item.getCategoriaId()
                ));

        Libro libro = libroRepository.findById(item.getLibroId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el libro con id " + item.getLibroId()
                ));

        CuraduriaEdicion curaduria = new CuraduriaEdicion();
        curaduria.setEdicion(edicion);
        curaduria.setCategoria(categoria);
        curaduria.setLibro(libro);
        curaduria.setPrecioVigente(item.getPrecioVigente());
        curaduria.setCupoMaximo(item.getCupoMaximo());

        curaduriaEdicionRepository.save(curaduria);
    }
    /**
     * Lista las curadurias (categoria, libro, precio, cupo) de una edicion puntual.
     */
    public List<CuraduriaEdicion> listarCuraduriasDeEdicion(Long edicionId) {
        if (!edicionRepository.existsById(edicionId)) {
            throw new IllegalArgumentException("No existe la edicion con id " + edicionId);
        }
        return curaduriaEdicionRepository.findByEdicionId(edicionId);
    }
}