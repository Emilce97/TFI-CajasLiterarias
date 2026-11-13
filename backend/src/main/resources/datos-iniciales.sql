-- ============================================
-- Cajas Literarias — datos-iniciales.sql
-- Datos minimos para arrancar una base nueva: las 4 categorias, el proveedor y libros de ejemplo.
-- Se corre a mano despues de schema.sql.
-- Se puede correr mas de una vez: cada INSERT verifica antes que el dato no exista (no duplica).
--
-- La administradora NO se carga aca: su contrasena no debe quedar en el repositorio.
-- Se crea sola al arrancar el backend con ADMIN_EMAIL y ADMIN_PASSWORD.
-- ============================================

SET NAMES utf8mb4;

-- Categorias: son 4 fijas por regla de negocio (no tienen pantalla de alta).
-- La imagen de cada caja en el frontend se elige segun este nombre.
INSERT INTO categoria (nombre)
SELECT 'Misterio/Terror' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Misterio/Terror');

INSERT INTO categoria (nombre)
SELECT 'Romance' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Romance');

INSERT INTO categoria (nombre)
SELECT 'Narrativa/Drama' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Narrativa/Drama');

INSERT INTO categoria (nombre)
SELECT 'Sorpresa' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Sorpresa');

-- Proveedor
INSERT INTO proveedor (nombre, contacto)
SELECT 'Editorial Sudamericana', 'contacto@sudamericana.com' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM proveedor WHERE nombre = 'Editorial Sudamericana');

-- Libros de ejemplo (todos del proveedor anterior).
INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'El Aleph', 'Jorge Luis Borges', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'El Aleph');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Orgullo y prejuicio', 'Jane Austen', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Orgullo y prejuicio');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Cien años de soledad', 'Gabriel García Márquez', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Cien años de soledad');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Rayuela', 'Julio Cortázar', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Rayuela');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Drácula', 'Bram Stoker', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Drácula');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Asesinato en el Orient Express', 'Agatha Christie', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Asesinato en el Orient Express');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'El extraño caso del Dr. Jekyll y Mr. Hyde', 'Robert Louis Stevenson', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'El extraño caso del Dr. Jekyll y Mr. Hyde');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Jane Eyre', 'Charlotte Brontë', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Jane Eyre');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Cumbres borrascosas', 'Emily Brontë', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Cumbres borrascosas');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'El túnel', 'Ernesto Sabato', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'El túnel');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Pedro Páramo', 'Juan Rulfo', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Pedro Páramo');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'El principito', 'Antoine de Saint-Exupéry', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'El principito');

INSERT INTO libro (titulo, autor, proveedor_id)
SELECT 'Ficciones', 'Jorge Luis Borges', p.id FROM proveedor p
WHERE p.nombre = 'Editorial Sudamericana'
  AND NOT EXISTS (SELECT 1 FROM libro WHERE titulo = 'Ficciones');