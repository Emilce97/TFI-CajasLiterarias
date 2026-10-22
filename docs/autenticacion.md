# Autenticación y roles

Hay dos roles: **ADMIN** (la administradora) y **SUSCRIPTOR**. `POST /api/auth/login` valida el email y la contraseña (guardada como hash BCrypt) contra ambas tablas y devuelve un token JWT firmado con `JWT_SECRET`, con el rol adentro. `POST /api/auth/registro` crea la cuenta de un suscriptor y devuelve el token (queda logueado). El frontend guarda la sesión en `localStorage` y envía el token en cada pedido como `Authorization: Bearer <token>`. El backend valida el token en cada request a una ruta protegida.

## Variables de entorno
| Variable | Uso |
|---|---|
| `JWT_SECRET` | Clave para firmar los tokens. 32 caracteres o más. Sin ella el backend no arranca. |
| `ADMIN_EMAIL` | Email de la administradora inicial. |
| `ADMIN_PASSWORD` | Contraseña inicial. |
| `DB_USERNAME`, `DB_PASSWORD` | Acceso a MySQL. |
| `FRONTEND_URL` | Origen permitido para CORS (la dirección del frontend). Por defecto, `http://localhost:5173`. |

`ADMIN_PASSWORD` solo se aplica la primera vez: si la administradora ya tiene contraseña, cambiar la variable no la actualiza. Para resetearla, poner `password_hash` en NULL en esa fila de `administradora` y reiniciar el backend.

## Cómo ingresar
- **Administradora:** `/login.html` con `ADMIN_EMAIL` y `ADMIN_PASSWORD`.
- **Suscriptor:** se registra en `/registro.html` (nombre, email, contraseña de 8 a 72 caracteres y dirección de entrega) o ingresa en `/login.html`.
El token dura 120 minutos.

## Base de datos existente
Si la base ya existía antes de estos cambios, correr una vez:
`ALTER TABLE administradora ADD COLUMN password_hash VARCHAR(100) NULL;`
`ALTER TABLE suscriptor ADD COLUMN password_hash VARCHAR(100) NULL;`
Los suscriptores creados antes del registro no tienen contraseña y no pueden ingresar (se borran o se vuelven a crear con el registro).


## Rutas
| Acceso | Rutas |
|---|---|
| Público | `POST /api/auth/login`, `POST /api/auth/registro`, `GET /api/categorias`, `GET /api/ediciones` (a quien no es ADMIN solo le devuelve la edición abierta), `GET /api/ediciones/{id}/curadurias` |
| SUSCRIPTOR | `POST /api/suscripciones`, `GET /api/suscripciones/mis-suscripciones`, `PATCH /api/suscripciones/{id}/baja\|pausa\|reanudar\|categoria` (solo sobre las propias), `POST /api/pagos` (solo sobre suscripciones propias), `GET` y `PATCH /api/suscriptores/mi-perfil` |
| ADMIN | `POST /api/ediciones`, `POST /api/ediciones/{id}/cierre`, `GET /api/pagos/pendientes`, `PATCH /api/pagos/{id}/validar`, `GET /api/ediciones/{id}/demanda`, `PATCH /api/demanda/{id}/recepcion`, `GET /api/ediciones/{id}/pedidos`, `PATCH /api/pedidos/{id}/estado`, `GET /api/pedidos/{id}/remito` |
- Cualquier ruta nueva queda reservada a ADMIN (403 para un suscriptor, 401 sin sesión) hasta que se la declare explícitamente.

## Alcance y limitaciones
- Un suscriptor solo puede operar sobre sus propias suscripciones y pagos: si el id es de otra persona (o no existe) responde 403.
- Un email no puede ser a la vez de una administradora y de un suscriptor.
- El registro revela si un email ya está registrado (409).
- No hay refresh token, recuperación de contraseña, verificación de email ni límite de intentos de login.
- El token se guarda en `localStorage` (expuesto a XSS). El guard del frontend es solo comodidad de interfaz: la protección real está en el backend.