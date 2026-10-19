# Autenticación de la administradora

`POST /api/auth/login` valida el email y la contraseña (guardada como hash BCrypt) y devuelve un token JWT firmado con `JWT_SECRET`. El frontend guarda la sesión en `localStorage` y envía el token en cada pedido como `Authorization: Bearer <token>`. El backend valida el token en cada request a una ruta de administración.

El backend protege las rutas de administración con JWT y rol `ADMIN`.

## Variables de entorno
| Variable | Uso |
|---|---|
| `JWT_SECRET` | Clave para firmar los tokens. 32 caracteres o más. Sin ella el backend no arranca. |
| `ADMIN_EMAIL` | Email de la administradora inicial. |
| `ADMIN_PASSWORD` | Contraseña inicial. |
| `DB_USERNAME`, `DB_PASSWORD` | Acceso a MySQL. |
| `FRONTEND_URL` | Origen permitido para CORS (la dirección del frontend). Por defecto, `http://localhost:5173`. |

`ADMIN_PASSWORD` solo se aplica la primera vez: si la administradora ya tiene contraseña, cambiar la variable no la actualiza. Para resetearla, poner `password_hash` en NULL en esa fila de `administradora` y reiniciar el backend.

Si la base ya existía antes de este cambio, correr una vez:
`ALTER TABLE administradora ADD COLUMN password_hash VARCHAR(100) NULL;`

## Cómo ingresar
Entrar a `/login.html` (o al enlace "Ingresar" del inicio) con `ADMIN_EMAIL` y `ADMIN_PASSWORD`. El token dura 120 minutos.

## Rutas
- **Públicas:** `POST /api/auth/login`, `GET /api/categorias`, `GET /api/ediciones` (a quien no es ADMIN solo le devuelve la edición abierta), `GET /api/ediciones/{id}/curadurias`, `POST /api/suscripciones`, `PATCH /api/suscripciones/**`, `POST /api/pagos`.
- **Solo ADMIN:** `POST /api/ediciones`, `POST /api/ediciones/{id}/cierre`, `GET /api/pagos/pendientes`, `PATCH /api/pagos/{id}/validar`, `GET /api/ediciones/{id}/demanda`, `PATCH /api/demanda/{id}/recepcion`, `GET /api/ediciones/{id}/pedidos`, `PATCH /api/pedidos/{id}/estado`, `GET /api/pedidos/{id}/remito`.
- Cualquier ruta nueva queda protegida por defecto (401) hasta que se la declare pública.

## Alcance y limitaciones
- En esta etapa solo existe el rol ADMIN. Las suscripciones y pagos del suscriptor son públicos.
- No hay refresh token, recuperación de contraseña ni límite de intentos de login.
- El token se guarda en `localStorage`. El guard del frontend es solo comodidad de interfaz: la protección real está en el backend.