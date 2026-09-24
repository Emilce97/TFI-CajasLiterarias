# Trabajo Final Integrador — Primera Entrega

> Plataforma de Gestión para Suscripción de Cajas Literarias

## 1. Identificación del Problema y Propuesta de Solución

### 1.1. Contexto y problema central

Se propone desarrollar una solución tecnológica para un microemprendimiento (caso simulado) de suscripción mensual a cajas de libros temáticos, con cuatro líneas: Misterio/Terror, Romance, Narrativa/Drama y Caja Sorpresa. Esta última funciona con la misma lógica que las demás: un único título curado y cerrado por mes por las administradoras, sin motores ni algoritmos de recomendación personalizada. Cada caja incluye el libro del mes, dos regalos fijos (señalador + QR con playlist) y regalos extra rotativos.

El emprendimiento ya cuenta con 100 suscriptores activos (escala de referencia para dimensionar el caso) y funciona bajo un ciclo mensual cerrado: día 1 al 20 para altas, bajas, pausas y cambios de temática; día 21 como fecha de corte, donde se congela el padrón y se reserva stock con proveedores; día 1 al 5 del mes siguiente para el despacho masivo.

Hoy todo esto se gestiona manualmente con WhatsApp y planillas de Excel, sin ningún sistema que centralice o valide la información entre etapas. Esto genera cinco problemas recurrentes: confusión en despachos (temática errónea), altas/bajas no sincronizadas, cambios de temática no reflejados a tiempo, riesgo de repetición de títulos, y cuellos de botella entre pagos validados y stock físico disponible.

**Planteamiento formal:** las administradoras de este emprendimiento, que gestionan todo el proceso manualmente, enfrentan el problema de que, a medida que crece la cantidad de suscriptores, la falta de sincronización entre etapas genera errores recurrentes, representando una pérdida estimada de entre $64.000 y $96.000 mensuales y entre 15 y 20 horas de trabajo dedicadas a validar información dispersa (cifras hipotéticas para dimensionar el problema). Una solución de software podría centralizar el estado real de cada suscriptor y automatizar las reglas del ciclo mensual, eliminando la necesidad de validar manualmente cada etapa.

A futuro, las administradoras planean incorporar una modalidad premium con un Club de Lectura Virtual (videollamadas y votación de próximas lecturas), funcionalidad que hoy no pueden sostener con las herramientas manuales que usan.

### 1.2. Actores involucrados

| Actor | Rol |
|---|---|
| Administradoras (emprendedoras) | Gestionan catálogo, stock, validación de pagos y despacho de pedidos. |
| Suscriptores (clientes) | Eligen temática, gestionan su envío y realizan el pago. |
| Proveedores de libros/insumos | Abastecen el stock según la demanda cerrada el día 21. Sin acceso al sistema. |
| Correo (envíos) | Actor externo tercerizado. El sistema llega hasta la generación del remito. |

### 1.3. Impacto y proceso actual

Al tratarse de un caso simulado, el impacto se estimó con ayuda de IA sobre la base de 100 suscriptores activos:

- **Tasa de error estimada:** ~8% de los envíos mensuales (≈8 casos), entre temática errónea, cambios no reflejados a tiempo y libros repetidos.
- **Costo económico:** entre $8.000 y $12.000 por caso, es decir, entre $64.000 y $96.000 mensuales.
- **Tiempo operativo perdido:** entre 15 y 20 horas mensuales dedicadas a cruzar WhatsApp con las planillas antes del corte del día 21.

El proceso actual, en resumen: el suscriptor pide el alta por WhatsApp y paga por transferencia → la administradora anota a mano el alta, el pago y la temática en Excel → durante el mes se cargan a mano las altas/bajas/cambios que van llegando → el día 21 se revisa la planilla completa a mano para pedir stock a proveedores → se arman y despachan los paquetes del 1 al 5 del mes siguiente, avisando por WhatsApp → cualquier reclamo posterior se resuelve buscando el error a mano en la planilla.

### 1.4. Valor de la solución

Una solución de software (y no simplemente una app de e-commerce) aporta valor porque ataca la falta de sincronización entre etapas, no solo la carga de datos:

- **Automatiza la fecha de corte:** el sistema congela el padrón el día 21 sin depender de que las administradoras actualicen la planilla a tiempo.
- **Historial unificado por suscriptor:** libros recibidos, pagos y temáticas en un mismo perfil, evitando repetición de títulos.
- **Trazabilidad de estado:** tanto administradoras como suscriptores ven en tiempo real cada etapa del pedido (pago → empaque → despacho con tracking → entrega).
- **Control automático de stock:** al congelar el padrón se obtiene la cantidad exacta de libros e insumos a pedir, evitando vender ejemplares inexistentes en ediciones con cupo limitado.

El problema es hipotético (no hay un cliente real), pero las reglas del negocio se pensaron en detalle para que fueran realistas. Como solución parcial existe el propio método actual (WhatsApp + Excel), que no alcanza porque ambas herramientas funcionan aisladas y no se validan entre sí: cualquier error de carga se traslada sin control a las etapas siguientes. Además, contar con una plataforma propia es lo que permitiría, a futuro, sostener mejoras como el Club de Lectura Virtual — hoy inviable con herramientas manuales.

---

## 2. Definición del Stack Tecnológico

| Capa | Tecnología | Justificación |
|---|---|---|
| Frontend | HTML, CSS y TypeScript (sin framework), con Vite como build tool | El equipo ya domina esta combinación de proyectos anteriores, y las pantallas requeridas (formularios y tablas, sin gestión de estado compleja) no justifican la curva de aprendizaje de un framework nuevo (React, Vue) en un contexto académico con fechas fijas. TypeScript por sobre JS puro se mantiene igual: su tipado estático ayuda a prevenir errores entre entidades relacionadas (suscriptor, suscripción, pedido, pago). |
| Backend | Java con Spring Boot | El equipo ya trabajó con Java y Hibernate/JPA el cuatrimestre anterior. Spring Boot no reemplaza ese conocimiento sino que lo potencia: integra Hibernate vía Spring Data JPA y resuelve de fábrica rutas REST, inyección de dependencias, validaciones y manejo centralizado de errores — tareas que en un proyecto de Java plano quedarían a cargo del equipo. |
| Persistencia | Spring Data JPA (Hibernate) | Integración nativa con Spring Boot sobre la base de conocimiento previo del equipo. |
| Base de datos | MySQL (relacional) | El dominio tiene relaciones fuertes y estables entre entidades (suscriptor-suscripción, edición-stock, pedido-pago), que se benefician de FKs, joins e integridad transaccional (ACID) — por ejemplo, evitar que se descuente stock sin un pago confirmado. Se descartó una alternativa no relacional porque no hay estructura de datos cambiante ni necesidad de escalar horizontalmente grandes volúmenes. Dentro de las opciones relacionales, se eligió MySQL (ya usado por el equipo en cuatrimestres anteriores) sobre PostgreSQL, que no aportaría ventaja concreta para la escala de este proyecto y sumaría una curva de aprendizaje innecesaria. |
| Despliegue | PaaS (Plataforma como Servicio) | Reduce la complejidad operativa de gestionar un servidor propio o una arquitectura basada en contenedores, algo que no aporta valor al objetivo del TFI dado el plazo académico y la falta de un equipo dedicado a infraestructura. Un PaaS permite subir el código y delegar la configuración del entorno (incluyendo MySQL como servicio adicional en muchos casos), priorizando el tiempo para el desarrollo funcional. |

**Escalabilidad y riesgos:** para la escala real del proyecto (100 suscriptores, sin picos de concurrencia relevantes), el stack elegido es más que suficiente — una arquitectura de microservicios o una base NoSQL sería sobreingeniería frente al volumen real de datos. Spring Boot y MySQL están preparados para sostener un crecimiento considerablemente mayor (miles de suscriptores) sin quedar cortos. Los riesgos identificados no son entonces de rendimiento, sino de tiempo de aprendizaje y puntos únicos de dependencia:

- **Ausencia de framework en el frontend:** a medida que crezcan las pantallas del sistema, puede volverse más difícil mantener el código organizado y evitar duplicación. Se mitiga manteniendo desde el inicio una estructura de carpetas y componentes reutilizables (HTML/TS).
- **Dependencia de un único gestor de base de datos:** migrar a futuro a un modelo no relacional (por ejemplo, para contenido más flexible como comentarios o valoraciones del Club de Lectura Virtual) implicaría un rediseño parcial del modelo de datos.

--- 

## 3. Refinamiento de propuesta y análisis de viabilidad asistida por IA

Esta sección muestra cómo se usó la IA como herramienta de apoyo real durante la etapa de propuesta: para poner a prueba la idea inicial, contrastarla con el contexto de mercado, y ordenar la planificación antes de comenzar el desarrollo — identificando puntos ciegos y cuestionando supuestos propios que de otra forma son difíciles de detectar solo.

### 3.1. Refinamiento de la idea y propuesta de valor

La idea inicial era general: un emprendimiento de cajas de suscripción de libros gestionado con WhatsApp y Excel, que necesitaría un sistema propio. Se usó la IA con la técnica de *prompting de rol* (actuando como una persona con experiencia en e-commerce y suscripciones) para detectar puntos ciegos, redundancias e inconsistencias en la lógica de negocio.

De ese intercambio surgieron dos observaciones que no estaban en la idea original:
- Faltaba una escala numérica de referencia (cantidad de suscriptores) para poder dimensionar stock, cupos y logística.
- Faltaba definir si la logística/correo formaba parte del sistema o quedaba tercerizada, y si la validación de pagos sería manual o vía pasarela real — ambas decisiones condicionan directamente el alcance del backend.

A partir de esto se fijó la base en 100 suscriptores activos, se definió la logística como actor externo tercerizado (la plataforma llega hasta el remito), y se optó por validación de pagos manual/semiautomática (evitando la complejidad de SDKs de pagos recurrentes dentro de los plazos del TFI).

Por último, se le pidió a la IA comparar dos estrategias para el ciclo de suscripciones: permitir cambios en cualquier momento del mes, vs. una fecha de corte fija que congele el padrón antes de pedir a proveedores. La IA recomendó la fecha de corte fija, por dos motivos: simplifica la lógica del sistema (evita modificaciones mientras se arman las cajas) y acerca el producto a la dinámica real de un club de suscripción, dándole identidad propia frente a una simple app de e-commerce. Esta recomendación fue la que definió el ciclo cerrado con corte el día 21 descripto en la Sección 1.

### 3.2. Análisis de competencia y diferenciación

Se usó la IA como asistente de investigación para un análisis de competencia preliminar (no exhaustivo, dado que el caso es simulado), contrastando la propuesta contra emprendimientos argentinos reales de cajas literarias: **Rizoma Libros, El Correo Lector, Bookerly y Jabutí** (competidores directos), más librerías con envío a domicilio y plataformas de lectura digital (competidores indirectos).

Se armó una matriz comparando precio, contenido de la caja, existencia de temáticas y canal de gestión, de donde surgieron dos hallazgos:
- La mayoría de los competidores arma sus cajas por sorpresa total (sin elegir género); ofrecer 4 líneas temáticas fijas es un diferenciador concreto.
- Ningún competidor relevado menciona públicamente una plataforma propia con trazabilidad de estado para el cliente — todos gestionan por WhatsApp o formularios simples, igual que el proceso manual que este proyecto busca reemplazar.

Al simular escenarios competitivos (por ejemplo, la aparición de un competidor gratuito), se concluyó que el valor sostenible en el tiempo no está en el precio ni en el contenido de la caja (fácilmente replicables), sino en la experiencia de gestión que ofrece la plataforma: trazabilidad del envío, historial de libros e historial de altas/bajas/cambios — algo que hoy ningún competidor resuelve de forma sistematizada.

### 3.3. Definición del MVP

**Incluido en el MVP:**
- Catálogo de cajas con sus cuatro líneas temáticas.
- Ciclo de suscripciones con fecha de corte automática (día 21).
- Gestión de altas, bajas, pausas y cambios de temática (ventana día 1 al 20).
- Gestión de cupos y stock por edición mensual.
- Validación manual de pagos, con panel de conciliación.
- Panel de despacho: Armado de pedidos, generación de remito interno, carga manual de código de seguimiento y actualización de estados del envío (Pendiente de empaque → Empaquetado → Despachado → Entregado).
- Historial de libros recibidos por suscriptor, para evitar repetición de títulos.

**Nice to have (si sobra tiempo):**
- **Recordatorios y avisos automáticos por correo:** Alertas preventivas programadas. Por ejemplo, recordatorio el día 17 sobre el vencimiento del plazo de pago el día 20 y aviso automático el día 21 informando si la suscripción ingresó a la edición o quedó pospuesta para el mes entrante por falta de pago. *(Nota: En el MVP, esta información se consulta de forma pasiva directamente en el panel web del suscriptor).*  
- Notificaciones automáticas por correo ante cambios de estado del pedido.
- Panel simple de reportes para las administradoras.

**No incluido (mejoras futuras):** integración con pasarela de pago real (Mercado Pago); Club de Lectura Virtual (nivel premium con videollamada y votación); integración por API con el correo para tracking automático — las tres se dejan fuera por la complejidad que agregarían frente a los plazos del TFI.

### 3.4. Plan de trabajo

Se usó nuevamente la IA (mismo prompting de rol) para generar un primer borrador del plan de trabajo a partir de la propuesta ya definida (problema, stack, MVP), pidiéndole que incluyera objetivos, alcance, tareas, riesgos/mitigaciones y criterios de éxito/fracaso. Luego se le pidió que formulara las preguntas necesarias para ajustar ese borrador a las restricciones reales del equipo (fechas de entrega, cantidad de integrantes, división de roles, horas semanales disponibles, herramienta de tracking), y con esas respuestas se afinó el cronograma final.

**Objetivo general:** desarrollar una plataforma web que centralice la gestión del ciclo de suscripciones, eliminando la dependencia de WhatsApp y Excel.

**Objetivos específicos:** automatizar la fecha de corte generando un snapshot inmutable del padrón; centralizar el historial de cada suscriptor; dar trazabilidad de estado a cada pedido desde el pago hasta la entrega.

**Alcance:** equipo de 2 integrantes, sin división fija de roles por tecnología, trabajando de forma conjunta sobre todas las capas (frontend, backend, base de datos, documentación). Disponibilidad estimada de 10 a 15 horas semanales combinadas. Seguimiento por commits/ramas del propio repositorio.

**Etapa 1 — Propuesta y repositorio (10/08–30/08):**  

Identificación del problema, stack tecnológico y refinamiento asistido por IA (este documento), más la creación del repositorio.

**Etapa 2 — Arquitectura y módulos (31/08–27/09):**

| Semana | Tarea |
|---|---|
| **1 (31/08–06/09)** | Diseño del modelo de datos (esquema ER con entidades desacopladas `Suscripcion` y `PedidoEdicion`), definición de módulos y setup inicial (Spring Boot + Vite/TS + MySQL). |
| **2 (07/09–13/09)** | Implementación backend del recorrido vertical: alta de suscripción, configuración de edición y operación manual idempotente de cierre/snapshot. |
| **3 (14/09–20/09)** | Conexión con frontend simple: visualización de ediciones, suscripción básica y prueba de inmutabilidad del padrón cerrado ante cambios posteriores. |
| **4 (21/09–27/09)** | Ajustes según feedback del tutor, buffer para pruebas funcionales del recorrido y entrega formal (condición de Regular). |

**Etapa 3 — Informe final, video y despliegue (28/09–14/11):**

| Semana | Tarea |
|---|---|
| **1 (28/09–04/10)** | Módulo de conciliación manual de pagos y reglas de exclusión al corte. |
| **2 (05/10–11/10)** | Módulo de Stock/Cupos y cálculo de demanda para proveedores. |
| **3 (12/10–18/10)** | Panel de despacho, remitos y estados de entrega. |
| **4 (19/10–25/10)** | Refinamiento de interfaces y componentes reutilizables en frontend. |
| **5 (26/10–01/11)** | Pruebas integrales y verificación de no repetición de títulos. |
| **6 (02/11–08/11)** | Despliegue en PaaS (Frontend y Backend/MySQL en la nube) + corrección de bugs. |
| **7 (09/11–14/11)** | Revisión general del proyecto y del informe final, grabación del video explicativo y entrega. |

**Riesgos principales y mitigación:** el riesgo más relevante es que la aprobación del tutor sobre el esquema de datos y módulos (fin de Etapa 2) pida cambios importantes — se mitiga presentando avances parciales antes de la fecha límite y dejando la semana 4 como buffer real. Con 10-15 hs/semana entre dos personas, también existe riesgo de atraso por imprevistos o módulos más complejos de lo previsto — se prioriza siempre el MVP sobre lo "nice to have", replanteando el cronograma apenas se detecta el desvío. Al no usar una herramienta de tracking externa, se usa el historial de commits como registro de avance semanal (con Trello como opción de respaldo si hiciera falta).

**Criterios de éxito:** ciclo mensual completo funcionando de punta a punta sin corrección manual de datos; historial por suscriptor evitando correctamente el reenvío de títulos; MVP completo desplegado en PaaS antes del 14/11; repositorio con README completo y documentación al día.

**Criterios de fracaso:** no lograr el ciclo completo funcionando para la entrega final, o MVP incompleto/sin desplegar en la nube. 

### 3.5. Análisis de viabilidad

- **Técnica:** el equipo tiene experiencia previa en Java, Hibernate/JPA y TypeScript, lo que reduce el riesgo de no poder implementar la solución con el stack elegido. La única dependencia externa relevante es el hosting vía PaaS, ya justificada en la Sección 2.
- **Operativa:** existen condiciones reales para adoptar la solución, ya que reemplaza directamente un proceso manual que el emprendimiento (simulado) ya ejecuta hoy — no requiere que las administradoras aprendan un proceso nuevo, sino que las mismas tareas pasen a una plataforma centralizada.
- **Temporal:** el alcance del MVP se delimitó específicamente para ser compatible con los plazos académicos, dejando explícitamente fuera lo que agregaría mayor tiempo de desarrollo (pagos reales, integración con correo, Club de Lectura).

---

## 4. Repositorio GitHub

- https://github.com/Emilce97/TFI-CajasLiterarias.git