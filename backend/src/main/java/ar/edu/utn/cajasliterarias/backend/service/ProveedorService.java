package ar.edu.utn.cajasliterarias.backend.service;

import ar.edu.utn.cajasliterarias.backend.dto.CrearProveedorRequest;
import ar.edu.utn.cajasliterarias.backend.model.Proveedor;
import ar.edu.utn.cajasliterarias.backend.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Proveedores del catalogo (quienes le venden los libros a Cajas Literarias).
 * Datos mal cargados o nombre repetido -> IllegalArgumentException (400).
 */
@Service
@RequiredArgsConstructor
public class ProveedorService {

    private static final int LARGO_MAXIMO = 150; // igual que las columnas de la tabla proveedor

    private final ProveedorRepository proveedorRepository;

    public List<Proveedor> listarProveedores() {
        return proveedorRepository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Proveedor crearProveedor(CrearProveedorRequest request) {
        if (request == null || request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio.");
        }
        String nombre = request.nombre().trim();
        if (nombre.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El nombre del proveedor no puede superar los " + LARGO_MAXIMO + " caracteres.");
        }

        // El contacto es opcional: si viene vacio se guarda como null.
        String contacto = request.contacto() == null || request.contacto().isBlank()
                ? null
                : request.contacto().trim();
        if (contacto != null && contacto.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El contacto no puede superar los " + LARGO_MAXIMO + " caracteres.");
        }

        if (proveedorRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("Ya existe un proveedor con el nombre '" + nombre + "'.");
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(nombre);
        proveedor.setContacto(contacto);
        return proveedorRepository.save(proveedor);
    }
}