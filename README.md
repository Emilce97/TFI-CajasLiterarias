# Cajas Literarias — Trabajo Final Integrador

## Repositorio del Trabajo Final Integrador (TFI) — Grupo 173 — Tecnicatura Universitaria en Programación a Distancia (UTN).

Docente/Tutor: Oscar Londero 

Integrantes: Natalia Córdoba, Emilce Spital

### Índice
- Propuesta del proyecto
- Problema central
- Stack tecnológico
- Alcance del MVP
- Plan de trabajo
- Integrantes

### Propuesta del proyecto

Se propone el desarrollo de una plataforma de gestión para un microemprendimiento (caso simulado) de suscripción mensual a cajas de libros temáticas. El negocio ofrece cuatro líneas de suscripción (Misterio/Terror, Romance, Narrativa/Drama y Caja Sorpresa), cada una compuesta por el libro del mes, dos regalos fijos (señalador + QR con playlist) y regalos extra rotativos.

El emprendimiento cuenta actualmente con 100 suscriptores activos y funciona bajo un ciclo mensual cerrado:

Día 1 al 20: ventana para altas, bajas, pausas y cambios de temática.
Día 21 (fecha de corte): se congela el padrón de suscriptores y se reserva el stock con los proveedores.
Día 1 al 5 del mes siguiente: despacho masivo de los pedidos.

A futuro, las administradoras planean incorporar una modalidad premium con un Club de Lectura Virtual (videollamadas y votación de próximas lecturas), funcionalidad que hoy no pueden sostener con las herramientas manuales que usan.

### Actores involucrados
| Actor | Rol |
|---|---|
| Administradoras (emprendedoras) | Gestionan catálogo, stock, validación de pagos y despacho de pedidos. |
| Suscriptores (clientes) | Eligen temática, gestionan su envío y realizan el pago. |
| Proveedores de libros/insumos | Abastecen el stock según la demanda cerrada el día 21. Sin acceso al sistema. |
| Correo (envíos) | Actor externo tercerizado. El sistema llega hasta la generación del remito. |

## Problema central
Todo el ciclo de suscripciones se gestiona hoy de forma manual mediante WhatsApp y planillas de Excel, sin ningún sistema que centralice o valide la información entre etapas (altas, bajas, pagos, despacho). Esto genera problemas concretos y recurrentes: confusión en despachos, altas/bajas no sincronizadas, cambios de temática no reflejados a tiempo, riesgo de reenviar títulos ya entregados, y desajustes entre pagos validados y stock físico disponible.

Planteamiento formal: las administradoras de este emprendimiento, que gestionan todo el proceso manualmente, enfrentan el problema de que, a medida que crece la cantidad de suscriptores (actualmente 100 activos), la falta de sincronización entre etapas genera errores recurrentes, representando una pérdida estimada de entre $64.000 y $96.000 mensuales y entre 15 y 20 horas de trabajo dedicadas a validar información dispersa. Una solución de software podría centralizar el estado real de cada suscriptor y automatizar las reglas del ciclo mensual, eliminando la necesidad de validar manualmente cada etapa.

## Stack tecnológico
| Capa | Tecnología | Motivo principal |
|---|---|---|
| **Frontend** | HTML, CSS y TypeScript (sin framework), con **Vite** como build tool | El equipo ya domina esta combinación de proyectos anteriores; las pantallas requeridas (formularios y tablas) no justifican la curva de aprendizaje de un framework. |
| **Backend** | **Java** con **Spring Boot** | El equipo ya trabajó con Java y Hibernate/JPA. Spring Boot potencia ese conocimiento agregando controladores REST, inyección de dependencias y validaciones de fábrica. |
| **Persistencia** | Spring Data JPA (Hibernate) | Integración nativa con Spring Boot sobre la base de conocimiento previo del equipo. |
| **Base de datos** | **MySQL** (relacional) | El dominio tiene relaciones fuertes entre entidades que se benefician de claves foráneas e integridad transaccional (ACID). Es el motor que el equipo ya utilizó en cuatrimestres anteriores. |
| **Despliegue** | PaaS (Plataforma como Servicio) | Reduce la complejidad operativa de gestionar un servidor propio, adecuado para los plazos de un trabajo académico. |
### La justificación detallada de cada decisión se encuentra en el documento completo de esta entrega.

## Alcance del MVP

Incluido:

Catálogo de cajas con sus cuatro líneas temáticas.
Ciclo de suscripciones con fecha de corte automática (día 21).
Gestión de altas, bajas, pausas y cambios de temática.
Gestión de cupos y stock por edición mensual.
Validación manual/semiautomática de pagos (panel de conciliación).
Panel de despacho con trazabilidad de estado del pedido.
Historial de libros recibidos por suscriptor (evita repeticiones).

## Fuera de alcance (versión futura):

Integración con pasarela de pago real (Mercado Pago, Stripe, etc.).
Club de Lectura Virtual (nivel premium de suscripción, con videollamada y votación de lecturas).
Integración por API con el correo para tracking automático.

### Plan de trabajo

El plan se organiza en torno a las tres instancias de entrega definidas por la cátedra. El equipo (2 integrantes) trabaja de forma conjunta sobre todas las capas del proyecto, sin división fija de roles, con una disponibilidad estimada de 10 a 15 horas semanales combinadas.

- Objetivo general: desarrollar una plataforma web que centralice la gestión del ciclo de suscripciones, eliminando la dependencia de WhatsApp y planillas de Excel.

- Objetivos específicos:

- Automatizar la fecha de corte mensual (congelamiento automático del padrón el día 21).
- Centralizar el historial de cada suscriptor.
- Dar trazabilidad de estado a cada pedido.

### Etapa 1 — Propuesta y repositorio (10/08 al 30/08)

## Entrega actual: identificación del problema, definición del stack tecnológico, refinamiento de la propuesta asistido por IA, y creación de este repositorio.

### Etapa 2 — Arquitectura y módulos (31/08 al 27/09)
| Semana | Tarea |
|---|---|
| 1 (31/08–06/09) | Diseño del modelo de datos (esquema ER) y definición inicial de módulos. |
| 2 (07/09–13/09) | Documentación de módulos + setup inicial del proyecto (esqueleto backend/frontend, conexión a MySQL). |
| 3 (14/09–20/09) | Ajustes según feedback del tutor + diagrama ER final. |
| 4 (21/09–27/09) | Buffer para correcciones + entrega formal (condición de Regular). |

### Etapa 3 — Informe final, video y despliegue (28/09 al 14/11)
| Semana | Tarea |
|---|---|
| 1 (28/09–04/10) | Backend: módulos Catálogo y Suscripciones. |
| 2 (05/10–11/10) | Backend: módulo Pagos + lógica de corte automático. |
| 3 (12/10–18/10) | Backend: Stock/Despacho + inicio de frontend (Catálogo, Suscripción). |
| 4 (19/10–25/10) | Frontend: paneles de conciliación de pagos y despacho. |
| 5 (26/10–01/11) | Integración frontend-backend + pruebas funcionales del ciclo completo. |
| 6 (02/11–08/11) | Despliegue en PaaS + corrección de bugs. |
| 7 (09/11–14/11) | Informe final, video explicativo y revisión general. |

Integrantes
Natalia Córdoba — colaboradora
Emilce Spital — colaboradora
