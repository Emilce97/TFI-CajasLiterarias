package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.ActualizarPerfilRequest;
import ar.edu.utn.cajasliterarias.backend.dto.CajaRecibidaResponse;
import ar.edu.utn.cajasliterarias.backend.model.Suscriptor;
import ar.edu.utn.cajasliterarias.backend.repository.PedidoEdicionRepository;
import ar.edu.utn.cajasliterarias.backend.repository.SuscriptorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuscriptorService {

    private final SuscriptorRepository suscriptorRepository;
    private final PedidoEdicionRepository pedidoEdicionRepository;

    public SuscriptorService(SuscriptorRepository suscriptorRepository,
                             PedidoEdicionRepository pedidoEdicionRepository) {
        this.suscriptorRepository = suscriptorRepository;
        this.pedidoEdicionRepository = pedidoEdicionRepository;
    }

    @Transactional(readOnly = true)
    public Suscriptor obtenerPerfil(String email) {
        return buscar(email);
    }

    /**
     * Actualiza nombre y/o direccion del suscriptor logueado. Solo afecta a los pedidos que se generen
     * despues: PedidoEdicion congela nombre y direccion en cada corte, asi que los ya congelados no cambian.
     */
    @Transactional
    public Suscriptor actualizarPerfil(String email, ActualizarPerfilRequest request) {
        if (request == null || (request.getNombre() == null && request.getDireccion() == null)) {
            throw new IllegalArgumentException("Indicá el nombre o la dirección que querés actualizar.");
        }
        Suscriptor suscriptor = buscar(email);
        if (request.getNombre() != null) {
            suscriptor.setNombre(limpiar(request.getNombre(), "nombre", 150));
        }
        if (request.getDireccion() != null) {
            suscriptor.setDireccion(limpiar(request.getDireccion(), "dirección", 255));
        }
        return suscriptorRepository.save(suscriptor);
    }

    /** Historial de la suscriptora: todos sus pedidos, con el estado en que está cada caja. */
    @Transactional(readOnly = true)
    public List<CajaRecibidaResponse> listarCajasRecibidas(String email) {
        return pedidoEdicionRepository.findBySuscripcionSuscriptorEmailOrderByIdDesc(email).stream()
                .map(CajaRecibidaResponse::desde)
                .toList();
    }

    private Suscriptor buscar(String email) {
        return suscriptorRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe un suscriptor con ese email."));
    }

    private String limpiar(String valor, String campo, int maximo) {
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede estar vacío.");
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }
}
