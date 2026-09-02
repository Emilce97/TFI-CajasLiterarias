# Cajas Literarias — Trabajo Final Integrador

> Repositorio del Trabajo Final Integrador (TFI) — **Grupo 173**  
> **Carrera:** Tecnicatura Universitaria en Programación a Distancia (UTN)  
> **Docente/Tutor:** Oscar Londero  
> **Integrantes:** Natalia Córdoba, Emilce Spital  

---

## Índice
1. Propuesta del proyecto  
2. Actores Involucrados  
3. Problema central  
4. Stack tecnológico   
5. Alcance del MVP  
6. Plan de trabajo   
7. Integrantes  

---

## Propuesta del proyecto  

Se propone el desarrollo de una plataforma de gestión para un microemprendimiento (caso simulado) de suscripción mensual a cajas de libros temáticas. El negocio ofrece cuatro líneas de suscripción:  
* **Misterio/Terror**
* **Romance**
* **Narrativa/Drama**  
* **Caja Sorpresa** Funciona con la misma lógica que las demás líneas: cuenta con un **único título curado y cerrado por mes** por las administradoras (novela gráfica, clásico, infantil ilustrado, etc.) para todos los suscriptores de la temática. No utiliza motores ni algoritmos de recomendación personalizada.

Cada una compuesta por el **libro seleccionado del mes**, **dos regalos fijos** (señalador + QR con playlist) y **regalos extra** rotativos.

### Regla de Negocio: 

Para dimensionar el proyecto se estableció una escala de referencia de 100 suscriptores activos (supuesto de simulación para modelar el caso), funcionando bajo un ciclo mensual cerrado:  
* **Día 1 al 20:** ventana para altas, bajas, pausas y cambios de temática.
* **Día 21 (fecha de corte):** se congela el padrón en un snapshot inmutable (PedidoEdicion) y se reserva el stock con los proveedores. Los cambios de temática o bajas solicitados con posterioridad a esta fecha impactan recién en la edición del mes siguiente.  
* **Día 1 al 5 del mes siguiente:** despacho masivo de los pedidos.

A futuro, las administradoras planean incorporar una modalidad premium con un Club de Lectura Virtual (videollamadas y votación de próximas lecturas), funcionalidad que hoy no pueden sostener con las herramientas manuales que usan.

---

## Actores involucrados  

| Actor | Rol |
| :--- | :--- |
| **Administradoras (emprendedoras)** | Gestionan catálogo, stock, validación de pagos y despacho de pedidos. |
| **Suscriptores (clientes)** | Eligen temática, gestionan su envío y realizan el pago. |
| **Proveedores de libros/insumos** | Abastecen el stock según la demanda cerrada el día 21. Sin acceso al sistema. |
| **Correo (envíos)** | Actor externo tercerizado. El sistema llega hasta la generación del remito. |

---

## Problema central  

Todo el ciclo de suscripciones se gestiona hoy de forma manual mediante WhatsApp y planillas de Excel, sin ningún sistema que centralice o valide la información entre etapas (altas, bajas, pagos, despacho). Esto genera problemas concretos y recurrentes: confusión en despachos, altas/bajas no sincronizadas, cambios de temática no reflejados a tiempo, riesgo de reenviar títulos ya entregados, y desajustes entre pagos validados y stock físico disponible.

> **Planteamiento formal:** las administradoras de este emprendimiento, que gestionan todo el proceso manualmente, enfrentan el problema de que, a medida que crece la cantidad de suscriptores (100 activos estimados como supuesto de simulación), la falta de sincronización entre etapas genera errores recurrentes, representando una pérdida estimada de entre $64.000 y $96.000 mensuales y entre 15 y 20 horas de trabajo dedicadas a validar información dispersa(cifras hipotéticamente construidas para dimensionar el problema). Una solución de software podría centralizar el estado real de cada suscriptor y automatizar las reglas del ciclo mensual, eliminando la necesidad de validar manualmente cada etapa.

---

## Stack tecnológico
| Capa | Tecnología | Motivo principal |
| :--- | :--- | :--- |
| **Frontend** | *HTML*, *CSS* y *TypeScript* (sin framework), con *Vite* como build tool | El equipo ya domina esta combinación de proyectos anteriores; las pantallas requeridas (formularios y tablas) no justifican la curva de aprendizaje de un framework. |
| **Backend** | *Java* con *Spring Boot* | El equipo ya trabajó con Java y Hibernate/JPA. Spring Boot potencia ese conocimiento agregando controladores REST, inyección de dependencias y validaciones de fábrica. |
| **Persistencia** | *Spring Data JPA (Hibernate)* | Integración nativa con Spring Boot sobre la base de conocimiento previo del equipo. |
| **Base de datos** | *MySQL* (relacional) | El dominio tiene relaciones fuertes entre entidades que se benefician de claves foráneas e integridad transaccional (ACID). Es el motor que el equipo ya utilizó en cuatrimestres anteriores. |
| **Despliegue** | *PaaS (Plataforma como Servicio)* | Reduce la complejidad operativa de gestionar un servidor propio, adecuado para los plazos de un trabajo académico. |  

*La justificación detallada de cada decisión se encuentra en el documento completo de esta entrega.*

---

## Alcance del MVP

### Incluido en el MVP

* **Catálogo de cajas:** Configuración de las cuatro líneas temáticas (Misterio/Terror, Romance, Narrativa/Drama y Caja Sorpresa como temática de título curado único por mes).  
* **Ciclo de suscripciones y snapshot de padrón:** Desacoplamiento entre la entidad continua `Suscripcion` y la entidad inmutable `PedidoEdicion` generada al momento del corte.  
* **Gestión de ciclo mensual:** Altas, bajas, pausas y cambios de temática en ventana habilitada (día 1 al 20).  
* **Corte estricto e idempotente (día 21):** Operación de congelamiento del padrón que incluye únicamente suscripciones con pago validado por la administradora. Suscripciones sin pago confirmado quedan excluidas de la edición y no reservan stock.  
* **Gestión de cupos y cálculo de stock:** Cálculo automático de la demanda de libros e insumos a solicitar a proveedores a partir del padrón cerrado de la edición.  
* **Validación manual de pagos:** Panel de conciliación para que las administradoras verifiquen transferencias y habiliten los pedidos antes del día 21.  
* **Panel de despacho:** Armado de pedidos, generación de remito interno, carga manual de código de seguimiento y actualización de estados del envío (Pendiente de empaque → Empaquetado → Despachado → Entregado).  
* **Historial de suscriptor:** Registro relacional de pedidos previos para prevenir repetición involuntaria de títulos.

### Nice to have (si sobra tiempo dentro del cronograma)  
* **Recordatorios y avisos automáticos por correo:** Alertas preventivas programadas (por ejemplo, recordatorio el día 17 sobre el vencimiento del plazo de pago el día 20 y aviso automático el día 21 informando si la suscripción ingresó a la edición o quedó pospuesta para el mes entrante por falta de pago. *(Nota: En el MVP, esta información se consulta de forma pasiva directamente en el panel web del suscriptor).*
* **Notificaciones por cambio de estado:** Correos automáticos para avisar al suscriptor sobre cambios de estado en su pedido (*Pago confirmado*, *Despachado con tracking*).
* **Panel de reportes básicos:** Visualización de métricas simples de despacho por temática y edición para las administradoras.

### No incluido en el MVP (mejoras futuras)
* **Pasarela de pago recurrente automática:** Integración directa por SDK (ej. Mercado Pago o Stripe) para suscripciones automáticas. Se deja fuera para no sumar complejidad asíncrona dentro de los plazos académicos.  
* **Club de Lectura Virtual (modalidad premium):** Encuentros por videollamada (Zoom/Meet) y sistema de votación dentro de la plataforma para próximas lecturas. Requiere modelar niveles de suscripción e integrar APIs externas.
* **Integración por API con correos:** Sincronización automática de tracking en tiempo real con empresas logísticas (el correo se modela como actor externo tercerizado).

---

## Plan de trabajo

El plan se organiza en torno a las tres instancias de entrega definidas por la cátedra. El equipo (2 integrantes) trabaja de forma conjunta sobre todas las capas del proyecto, sin división fija de roles, con una disponibilidad estimada de 10 a 15 horas semanales combinadas.

**Objetivo general:** desarrollar una plataforma web que centralice la gestión del ciclo de suscripciones, eliminando la dependencia de WhatsApp y planillas de Excel.

**Objetivos específicos:**

* Automatizar la fecha de corte mensual generando un snapshot inmutable del padrón (el día 21).
* Centralizar el historial de cada suscriptor evitando repetición de títulos.
* Dar trazabilidad de estado a cada pedido desde el pago hasta la entrega.

### Etapa 1 — Propuesta y repositorio (10/08 al 30/08)

**Entrega actual:** identificación del problema, definición del stack tecnológico, refinamiento de la propuesta asistido por IA, y creación de este repositorio.

### Etapa 2 — Arquitectura y módulos (31/08 al 27/09)

| Semana | Tarea |
| :---: | :--- |
| **1 (31/08–06/09)** | Diseño del modelo de datos (esquema ER con entidades desacopladas `Suscripcion` y `PedidoEdicion`), definición de módulos y setup inicial (Spring Boot + Vite/TS + MySQL). |
| **2 (07/09–13/09)** | Implementación backend del recorrido vertical: alta de suscripción, configuración de edición y operación manual idempotente de cierre/snapshot. |
| **3 (14/09–20/09)** | Conexión con frontend simple: visualización de ediciones, suscripción básica y prueba de inmutabilidad del padrón cerrado ante cambios posteriores. |
| **4 (21/09–27/09)** | Ajustes según feedback del tutor, buffer para pruebas funcionales del recorrido y entrega formal (**condición de Regular**). |

### Etapa 3 — Informe final, video y despliegue (28/09 al 14/11)

| Semana | Tarea |
| :---: | :--- |
| **1 (28/09–04/10)** | Módulo de conciliación manual de pagos y reglas de exclusión al corte. |
| **2 (05/10–11/10)** | Módulo de Stock/Cupos y cálculo de demanda para proveedores. |
| **3 (12/10–18/10)** | Panel de despacho, remitos y estados de entrega. |
| **4 (19/10–25/10)** | Refinamiento de interfaces y componentes reutilizables en frontend. |
| **5 (26/10–01/11)** | Pruebas integrales y verificación de no repetición de títulos |
| **6 (02/11–08/11)** | Despliegue en PaaS (Frontend y Backend/MySQL en la nube) + corrección de bugs. |
| **7 (09/11–14/11)** | Revisión general del proyecto y del informe final, grabación del video explicativo y entrega. |

> **Nota:** La documentación y el informe final se irán actualizando de manera incremental a medida que se completen los módulos, reservando la última semana exclusivamente para su consolidación, revisión general y grabación del video.  

---

## Integrantes  

* **Natalia Córdoba** — colaboradora
* **Emilce Spital** — colaboradora
