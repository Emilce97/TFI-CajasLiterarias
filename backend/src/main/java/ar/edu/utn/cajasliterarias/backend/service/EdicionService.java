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
     */
    @Transactional
    public Edicion crearEdicion(CrearEdicionRequest request) {

        if (edicionRepository.existsByNombre(request.getNombre())) {
            throw new IllegalArgumentException(
                    "Ya existe una edicion con el nombre '" + request.getNombre() + "'."
            );
        }

        if (request.getCuradurias() == null || request.getCuradurias().size() != 4) {
            throw new IllegalArgumentException(
                    "La edicion debe tener exactamente 4 curadurias (una por categoria)."
            );
        }

        Edicion edicion = new Edicion();
        edicion.setNombre(request.getNombre());
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
}