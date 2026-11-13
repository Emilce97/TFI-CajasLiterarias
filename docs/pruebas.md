# Pruebas

El sistema se validó en tres niveles: **tests automáticos** (corren en cada cambio), **pruebas manuales** en local sobre las pantallas y **prueba del circuito completo en producción**.

---

## 1. Tests automáticos

**334 tests en verde** (`./gradlew test` desde `backend/`), de tres tipos:

| Tipo | Qué verifican | Herramientas |
| :--- | :--- | :--- |
| **Lógica de negocio** | Reglas de cada servicio por separado: validaciones al crear una edición (4 curadurías, precio único, fechas, una sola edición abierta), registro y validación de pagos, corte (exclusiones por pago, cupo y curaduría; prioridad por fecha de validación), despacho paso a paso, remito, catálogo. | JUnit 5 + Mockito |
| **Seguridad y respuestas HTTP** | Que cada ruta responda con el código correcto (201, 400, 409) y el mensaje del error, y que esté protegida según el rol: **403** para un suscriptor en rutas de administración y **401** sin sesión. | `@WebMvcTest` + Spring Security Test |
| **Integración con la base real** | Flujos completos contra MySQL/MariaDB local, con los servicios reales. Cada test se revierte al terminar (`@Transactional`), así que no deja datos. | `@SpringBootTest` |

Tests de integración destacados:

| Test | Recorrido |
| :--- | :--- |
| `CircuitoAdminIntegrationTest` | Registrar pago → validarlo → corte → pedido en despacho → remito (rechazado antes de empaquetar) → empaquetado → despachado con tracking → entregado → la consulta de no repetición reconoce el libro como recibido. También: un pago sin validar queda fuera del corte. |
| `CircuitoSuscriptorIntegrationTest` | Registro → login → suscripción → cambio de temática → pago, por HTTP con token real. Incluye el rechazo del cambio de temática si ya recibió el libro y la marca de libro repetido en el corte. |
| `EstadoSuscripcionYCorteIntegrationTest` / `CambioCategoriaYCorteIntegrationTest` | Pausas, bajas y cambios de temática pedidos antes y desde el día del corte: cuáles afectan la edición que se cierra y cuáles rigen desde la siguiente. |
| `AvisoCorteIntegrationTest` | El aviso antes del corte detecta a quien recibiría un libro que ya tiene, ignora libros nuevos y no se pide sobre una edición cerrada. |
| `PagoUniqueIntegrationTest` | La base rechaza un segundo pago de la misma suscripción para la misma edición. |

El frontend se verifica con `npx tsc --noEmit` y `npm run build` (desde `frontend/`), que compilan sin errores.

---

## 2. Pruebas manuales (local)

| # | Módulo | Caso | Resultado esperado | Resultado |
| :---: | :--- | :--- | :--- | :---: |
| 1 | Ediciones | Crear una edición con datos incompletos o fechas inválidas (despacho antes del corte). | La pantalla muestra el motivo y no envía nada. | ✅ |
| 2 | Ediciones | Crear una edición con un nombre que ya existe, escrito en minúsculas. | El backend la rechaza y la pantalla muestra su mensaje ("Ya existe una edición con el nombre…"). | ✅ |
| 3 | Ediciones | Crear una edición válida. | Se crea abierta y el formulario desaparece (solo puede haber una abierta). | ✅ |
| 4 | Corte | Tocar "Cerrar edición" y cancelar la confirmación. | No pasa nada: la edición sigue abierta. | ✅ |
| 5 | Corte | Cerrar una edición de prueba sin pagos (con backup previo de la base). | Resumen: 0 pedidos y todas las suscripciones activas excluidas; la edición queda cerrada y vuelve a aparecer el formulario. | ✅ |
| 6 | No repetición | Abrir la edición actual con un libro que una suscriptora ya recibió. | La tarjeta de la edición muestra el aviso con su nombre y el libro, y la confirmación del corte lo repite. | ✅ |
| 7 | No repetición | Ver en Despacho un pedido marcado como repetido. | La columna "Caja" muestra la etiqueta "Libro repetido". | ✅ |
| 8 | Pagos | Una suscriptora informa un pago y la administradora lo valida. | El pago sale de pendientes y aparece en el historial con quién y cuándo lo validó. | ✅ |
| 9 | Catálogo | Agregar un proveedor sin nombre / con un nombre existente en minúsculas. | Mensaje de validación / rechazo por nombre repetido. | ✅ |
| 10 | Catálogo | Agregar un proveedor y un libro de ese proveedor. | Aparecen en las listas, y el libro queda disponible en el formulario de nueva edición. | ✅ |
| 11 | Mi suscripción | Ver "Mis cajas recibidas" sin cajas entregadas. | Mensaje explicando que todavía no hay cajas. | ✅ |
| 12 | Datos iniciales | Correr `datos-iniciales.sql` dos veces. | La segunda vez no duplica nada (13 libros en ambos casos). | ✅ |
| 13 | Responsive | Recorrer el sitio a 375 px y a 1024 px. | Menú hamburguesa en celular; las tablas de la administradora se ven como tarjetas; sin desbordes. | ✅ |

---

## 3. Prueba en producción

Circuito completo sobre la versión publicada (Render + TiDB):

| # | Paso | Resultado esperado | Resultado |
| :---: | :--- | :--- | :---: |
| 1 | Ingresar como administradora y crear una edición con libros del catálogo. | La edición se crea abierta. | ✅ |
| 2 | Registrar una suscriptora nueva, suscribirse a una temática e informar el pago. | Aparece la suscripción y el pago queda pendiente. | ✅ |
| 3 | Validar el pago como administradora. | El pago sale de pendientes y aparece en el historial de validados. | ✅ |
| 4 | Cerrar la edición y verificar el resumen del corte. | El resumen muestra 1 pedido generado y la edición queda cerrada. | ✅ |
| 5 | Avanzar el pedido en Despacho hasta Entregado y abrir el remito. | Cada cambio de estado se refleja y el remito muestra los datos congelados del pedido. | ✅ |
| 6 | Como suscriptora, ver la caja en "Mis cajas recibidas". | La caja entregada aparece en la lista. | ✅ |
| 7 | Ingresar con el email de la administradora en mayúsculas. | El ingreso funciona (el email se compara en minúsculas). | ✅ |
| 8 | Crear una edición con el nombre de una existente, cambiando las mayúsculas. | El backend la rechaza con "Ya existe una edición con el nombre…" (TiDB distingue mayúsculas por defecto). | ✅ |