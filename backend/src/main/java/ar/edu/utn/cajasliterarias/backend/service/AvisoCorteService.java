package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.PosibleRepeticionResponse;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoEdicion;
import ar.edu.utn.cajasliterarias.backend.enums.EstadoSuscripcion;
import ar.edu.utn.cajasliterarias.backend.model.CuraduriaEdicion;
import ar.edu.utn.cajasliterarias.backend.model.Edicion;
import ar.edu.utn.cajasliterarias.backend.model.Suscripcion;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.repository.CuraduriaEdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.EdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.PedidoEdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aviso antes del corte (no repeticion, capa 2): para la edicion abierta, lista las suscriptoras
 * activas que recibirian un libro que ya recibieron en una edicion anterior.
 * Asi la administradora puede cambiar el libro de la curaduria antes de cerrar la edicion.
 */
@Service
@RequiredArgsConstructor
public class AvisoCorteService {

    private final EdicionRepository edicionRepository;
    private final CuraduriaEdicionRepository curaduriaEdicionRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final PedidoEdicionRepository pedidoEdicionRepository;

    @Transactional(readOnly = true)
    public List<PosibleRepeticionResponse> posiblesRepeticiones(Long edicionId) {
        Edicion edicion = edicionRepository.findById(edicionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la edición con id " + edicionId));
        if (edicion.getEstado() != EstadoEdicion.ABIERTA) {
            throw new IllegalStateException("La edición ya está cerrada: el aviso es solo antes del corte.");
        }

        // Que libro le toca a cada tematica en esta edicion.
        Map<Long, CuraduriaEdicion> curaduriaPorCategoria = new HashMap<>();
        for (CuraduriaEdicion c : curaduriaEdicionRepository.findByEdicionId(edicionId)) {
            curaduriaPorCategoria.put(c.getCategoria().getId(), c);
        }

        List<PosibleRepeticionResponse> resultado = new ArrayList<>();
        // Todas las activas (no solo las que ya pagaron): todavia pueden pagar antes del corte.
        for (Suscripcion suscripcion : suscripcionRepository.findByEstado(EstadoSuscripcion.ACTIVA)) {
            CuraduriaEdicion curaduria = curaduriaPorCategoria.get(suscripcion.getCategoria().getId());
            if (curaduria == null) {
                continue;
            }
            Suscriptor suscriptora = suscripcion.getSuscriptor();
            boolean yaLoRecibio = pedidoEdicionRepository.existsBySuscripcionSuscriptorIdAndCuraduriaLibroId(
                    suscriptora.getId(), curaduria.getLibro().getId());
            if (yaLoRecibio) {
                resultado.add(new PosibleRepeticionResponse(
                        suscripcion.getId(),
                        suscriptora.getNombre(),
                        suscriptora.getEmail(),
                        curaduria.getCategoria().getNombre(),
                        curaduria.getLibro().getTitulo()));
            }
        }
        return resultado;
    }
}