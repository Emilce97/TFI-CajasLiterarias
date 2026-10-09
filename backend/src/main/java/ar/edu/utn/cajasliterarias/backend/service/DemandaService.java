package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.DemandaItemDTO;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
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
 * (CierreEdicionService); aca solo se consultan y, mas adelante, se registra la recepcion.
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
        // Para el panel de demanda: todas las filas (una por libro) de una edicion.
        return demandaEdicionRepository.findByEdicionId(edicionId).stream()
                .map(this::aDTO)
                .sorted(Comparator.comparing(DemandaItemDTO::getProveedorNombre)
                        .thenComparing(DemandaItemDTO::getTitulo))
                .toList();
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