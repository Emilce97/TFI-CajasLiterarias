-- ============================================
-- Cajas Literarias — schema.sql
-- ddl-auto=none: este script se corre a mano en la base de datos cajas_literarias
-- antes de levantar el backend.
-- ============================================

CREATE TABLE administradora (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE proveedor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    contacto VARCHAR(150)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE libro (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    autor VARCHAR(150),
    proveedor_id BIGINT,
    CONSTRAINT fk_libro_proveedor
        FOREIGN KEY (proveedor_id) REFERENCES proveedor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE suscriptor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    direccion VARCHAR(255),
    fecha_registro DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE edicion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    fecha_corte DATE NOT NULL,
    fecha_despacho_desde DATE,
    fecha_despacho_hasta DATE,
    estado VARCHAR(30) NOT NULL  -- ABIERTA / CERRADA
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla intermedia: libro curado por categoria en cada edicion.
-- Siempre debe existir una fila por cada categoria en cada edicion
-- (regla de negocio validada a nivel aplicacion, no en el schema).
CREATE TABLE curaduria_edicion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    edicion_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    libro_id BIGINT NOT NULL,
    precio_vigente DECIMAL(10,2) NOT NULL,
    cupo_maximo INT,
    CONSTRAINT fk_curaduria_edicion
        FOREIGN KEY (edicion_id) REFERENCES edicion(id),
    CONSTRAINT fk_curaduria_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    CONSTRAINT fk_curaduria_libro
        FOREIGN KEY (libro_id) REFERENCES libro(id),
    CONSTRAINT uq_curaduria_edicion_categoria
        UNIQUE (edicion_id, categoria_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE suscripcion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    suscriptor_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL, -- ACTIVA / PAUSADA / BAJA
    -- Cambio de tematica pedido despues del corte: no pisa 'categoria_id'
    -- hasta el proximo cierre.
    proxima_categoria_id  BIGINT,
    fecha_solicitud_cambio DATE,
    fecha_alta DATE,
    fecha_baja DATE,
    CONSTRAINT fk_suscripcion_suscriptor
        FOREIGN KEY (suscriptor_id) REFERENCES suscriptor(id),
    CONSTRAINT fk_suscripcion_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    CONSTRAINT fk_suscripcion_proxima_categoria
        FOREIGN KEY (proxima_categoria_id) REFERENCES categoria(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pago (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    suscripcion_id BIGINT NOT NULL,
    edicion_id BIGINT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_pago DATE,
    estado VARCHAR(30) NOT NULL, -- PENDIENTE / VALIDADO
    validado_por_admin_id BIGINT,
    fecha_validacion DATE,
    CONSTRAINT fk_pago_suscripcion
        FOREIGN KEY (suscripcion_id) REFERENCES suscripcion(id),
    CONSTRAINT fk_pago_edicion
        FOREIGN KEY (edicion_id) REFERENCES edicion(id),
    CONSTRAINT fk_pago_admin
        FOREIGN KEY (validado_por_admin_id) REFERENCES administradora(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Snapshot inmutable del padrón al momento del corte.
-- Una suscripcion entra como mucho una vez por edicion.
CREATE TABLE pedido_edicion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    suscripcion_id BIGINT NOT NULL,
    edicion_id BIGINT NOT NULL,
    pago_id BIGINT NOT NULL,
    curaduria_id BIGINT NOT NULL,
    categoria_congelada VARCHAR(50) NOT NULL,
    libro_congelado VARCHAR(255) NOT NULL,
    precio_aplicado DECIMAL(10,2) NOT NULL,
    direccion_entrega VARCHAR(255) NOT NULL,
    estado_pedido VARCHAR(30) NOT NULL, -- PENDIENTE_DE_EMPAQUE / EMPAQUETADO / DESPACHADO / ENTREGADO
    tracking VARCHAR(100),
    observaciones TEXT,
    modificado_por_admin_id BIGINT,
    fecha_modificacion DATE,
    fecha_creacion DATE NOT NULL,
    CONSTRAINT fk_pedido_suscripcion
        FOREIGN KEY (suscripcion_id) REFERENCES suscripcion(id),
    CONSTRAINT fk_pedido_edicion
        FOREIGN KEY (edicion_id) REFERENCES edicion(id),
    CONSTRAINT fk_pedido_pago
        FOREIGN KEY (pago_id) REFERENCES pago(id),
    CONSTRAINT fk_pedido_curaduria
        FOREIGN KEY (curaduria_id) REFERENCES curaduria_edicion(id),
    CONSTRAINT fk_pedido_admin
        FOREIGN KEY (modificado_por_admin_id) REFERENCES administradora(id),
    CONSTRAINT uq_pedido_suscripcion_edicion
        UNIQUE (suscripcion_id, edicion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla hermana de pedido_edicion: registra las suscripciones activas que
-- NO ingresaron a la edicion en el corte (por falta de pago validado).
-- Una suscripcion entra como mucho una vez por edicion, igual que pedido_edicion.
CREATE TABLE exclusion_edicion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    suscripcion_id BIGINT NOT NULL,
    edicion_id BIGINT NOT NULL,
    motivo VARCHAR(30) NOT NULL, -- SIN_PAGO / PAGO_PENDIENTE
    fecha_registro DATE NOT NULL,
    CONSTRAINT fk_exclusion_suscripcion
        FOREIGN KEY (suscripcion_id) REFERENCES suscripcion(id),
    CONSTRAINT fk_exclusion_edicion
        FOREIGN KEY (edicion_id) REFERENCES edicion(id),
    CONSTRAINT uq_exclusion_suscripcion_edicion
        UNIQUE (suscripcion_id, edicion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Agregado de pedido_edicion por libro/edicion. Se calcula, no se carga a mano.
CREATE TABLE demanda_edicion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    edicion_id BIGINT NOT NULL,
    libro_id BIGINT NOT NULL,
    cantidad_requerida INT NOT NULL,
    cantidad_recibida INT DEFAULT 0,
    estado_faltante BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_demanda_edicion
        FOREIGN KEY (edicion_id) REFERENCES edicion(id),
    CONSTRAINT fk_demanda_libro
        FOREIGN KEY (libro_id) REFERENCES libro(id),
    CONSTRAINT uq_demanda_edicion_libro
        UNIQUE (edicion_id, libro_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;