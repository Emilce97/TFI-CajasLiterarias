package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.DemandaItemDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoFaltante;
import ar.edu.utn.cajasliterarias.backend.model.DemandaEdicion;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.Libro;
import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import ar.edu.utn.cajasliterarias.backend.repository.DemandaEdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Demanda de libros para proveedores. Las filas las genera el corte
 * (CierreEdicionService); aca se consultan y se registra lo que llega de cada proveedor.
 */
@Service
public class DemandaService {

    private static final String SIN_PROVEEDOR = "Sin proveedor asignado";

    private final DemandaEdicionRepository demandaEdicionRepository;
    private final EdicionRepository edicionRepository;

    public DemandaService(DemandaEdicionRepository demandaEdicionRepository,
                          EdicionRepository edicionRepository) {
        this.demandaEdicionRepository = demandaEdicionRepository;
        this.edicionRepository = edicionRepository;
    }

    /**
     * Devuelve la demanda de una edicion ya cerrada, ordenada por proveedor y titulo,
     * para que la administradora sepa cuantos libros pedirle a cada proveedor.
     */
    @Transactional(readOnly = true)
    public List<DemandaItemDTO> listarDemandaDeEdicion(Long edicionId) {
        Edicion edicion = edicionRepository.findById(edicionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la edicion con id " + edicionId));

        // La demanda se calcula en el corte: una edicion abierta todavia no la tiene.
        if (edicion.getEstado() == EstadoEdicion.ABIERTA) {
            throw new IllegalStateException(
                    "La edicion " + edicionId + " sigue abierta: la demanda se calcula al cerrarla.");
        }

        return demandaEdicionRepository.findByEdicionId(edicionId).stream()
                .map(this::aDTO)
                .sorted(Comparator.comparing(DemandaItemDTO::getProveedorNombre)
                        .thenComparing(DemandaItemDTO::getTitulo))
                .toList();
    }

    /**
     * Registra cuantos ejemplares de un libro llegaron del proveedor.
     * La cantidad es el TOTAL recibido (reemplaza al valor anterior) y el estado de faltante
     * se recalcula: FALTANTE si llego menos de lo requerido, SIN_FALTANTE si se completo.
     */
    @Transactional
    public DemandaItemDTO registrarRecepcion(Long demandaId, Integer cantidadRecibida) {
        if (cantidadRecibida == null || cantidadRecibida < 0) {
            throw new IllegalArgumentException("La cantidad recibida es obligatoria y no puede ser negativa.");
        }

        DemandaEdicion demanda = demandaEdicionRepository.findById(demandaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la demanda con id " + demandaId));

        int requerida = valorOCero(demanda.getCantidadRequerida());
        demanda.setCantidadRecibida(cantidadRecibida);
        demanda.setEstadoFaltante(cantidadRecibida < requerida
                ? EstadoFaltante.FALTANTE
                : EstadoFaltante.SIN_FALTANTE);

        return aDTO(demandaEdicionRepository.save(demanda));
    }

    private DemandaItemDTO aDTO(DemandaEdicion demanda) {
        Libro libro = demanda.getLibro();
        Proveedor proveedor = libro.getProveedor();
        int requerida = valorOCero(demanda.getCantidadRequerida());
        int recibida = valorOCero(demanda.getCantidadRecibida());

        DemandaItemDTO dto = new DemandaItemDTO();
        dto.setId(demanda.getId());
        dto.setLibroId(libro.getId());
        dto.setTitulo(libro.getTitulo());
        dto.setAutor(libro.getAutor());
        dto.setProveedorId(proveedor != null ? proveedor.getId() : null);
        dto.setProveedorNombre(proveedor != null ? proveedor.getNombre() : SIN_PROVEEDOR);
        dto.setCantidadRequerida(requerida);
        dto.setCantidadRecibida(recibida);
        dto.setCantidadFaltante(Math.max(requerida - recibida, 0));
        dto.setEstadoFaltante(demanda.getEstadoFaltante());
        return dto;
    }

    private int valorOCero(Integer valor) {
        return valor != null ? valor : 0;
    }
}