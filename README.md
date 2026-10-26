# Lava Rápido Vehicular — API backend

API REST de la plataforma Lava Rápido Vehicular. Gestiona usuarios, vehículos, servicios, reservas, asignaciones, pagos, notificaciones, calificaciones y auditoría administrativa. Es una aplicación Spring Boot; el esquema de PostgreSQL y sus migraciones Liquibase se administran por separado en el proyecto de base de datos.

> Nota operativa: actualmente, `EmailService` requiere `BREVO_API_KEY` al iniciar la aplicación. Esta variable no aparece en `.env.example` ni se transfiere mediante `docker-compose.yml`. Debe configurarse explícitamente en el entorno de ejecución. Esta documentación describe la situación actual; no corrige esa brecha de configuración.

## Contenido

1. [Arquitectura](#arquitectura)
2. [Tecnologías](#tecnologías)
3. [Módulos](#módulos)
4. [Configuración local](#configuración-local)
5. [Configuración](#configuración)
6. [Referencia de la API](#referencia-de-la-api)
7. [Autenticación y autorización](#autenticación-y-autorización)
8. [Ciclo de vida de una reserva](#ciclo-de-vida-de-una-reserva)
9. [Cancelación y aceptación de la política](#cancelación-y-aceptación-de-la-política)
10. [Pagos con Wompi](#pagos-con-wompi)
11. [Notificaciones](#notificaciones)
12. [Base de datos y administración del esquema](#base-de-datos-y-administración-del-esquema)
13. [Perfiles, Docker y despliegue](#perfiles-docker-y-despliegue)
14. [Verificaciones de salud y observabilidad](#verificaciones-de-salud-y-observabilidad)
15. [Errores](#errores)
16. [Pruebas y limitaciones](#pruebas-y-limitaciones)

## Arquitectura

La aplicación sigue una arquitectura Spring por capas:

- `controller`: rutas HTTP, vinculación de solicitudes y códigos de respuesta.
- `service`: reglas de negocio, comprobaciones de propiedad, transacciones, coordinación de pagos y notificaciones.
- `repository`: acceso a PostgreSQL mediante Spring Data JPA.
- `model` / `entity`: tipos del dominio persistidos y enumeraciones.
- `dto`: contratos de solicitudes y respuestas de la API.
- `shared/config` y `shared/security`: configuración, autenticación JWT, autorización, CORS y manejo de excepciones.

La aplicación no mantiene sesiones de usuario. Las solicitudes se autentican con JWT; el rol y el estado de la cuenta se comprueban contra la base de datos. Las notificaciones de Wompi ingresan por el endpoint webhook de pagos y se verifican antes de modificar el estado de un pago.

## Tecnologías

| Componente | Versión o procedencia |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.0.9 |
| Maven Wrapper | 3.3.4; distribución de Maven 3.9.14 |
| Spring MVC, Security, Data JPA, Validation, Actuator y Mail | Versiones administradas por Spring Boot 4.0.9 |
| Controlador JDBC de PostgreSQL | Versión administrada por Spring Boot |
| JJWT | 0.12.6 |
| springdoc OpenAPI UI | 3.0.2 |
| Swagger UI WebJar | 5.32.14 |
| Esquema y migraciones PostgreSQL | Se mantienen en el proyecto independiente `lavarapido-base-datos`; no están incluidos en esta aplicación |

Las versiones indicadas se verificaron en el POM, la configuración del wrapper y la configuración de la aplicación. Las versiones transitivas se administran mediante dependencias y no se enumeran como versiones fijadas individualmente.

## Módulos

| Área | Responsabilidad principal |
| --- | --- |
| Usuarios y autenticación | Registro, inicio de sesión, perfil, cambios de contraseña/correo y recuperación de contraseña |
| Marcas, servicios y vehículos | Administración del catálogo y propiedad de vehículos |
| Reservas | Ciclo de vida, propiedad, cancelación y auditoría de reservas |
| Asignaciones y operadores | Asignación administrativa y actualización del trabajo del operador |
| Pagos | Intentos de pago Wompi, verificación, procesamiento de webhooks y conciliación |
| Notificaciones | Notificaciones internas, tokens de dispositivos y envío opcional de correo/notificaciones push |
| Calificaciones | Comentarios sobre reservas |
| Auditoría y registros de errores | Historial administrativo y revisión de errores |

## Configuración local

Requisitos: JDK 17, PowerShell en Windows (u otra consola compatible) y acceso a una base de datos PostgreSQL cuyo esquema ya haya sido preparado por el proyecto de base de datos. Hibernate usa `ddl-auto=validate`; el backend no crea ni migra el esquema.

1. Configura las variables de entorno de la siguiente sección. No incluyas secretos ni credenciales reales en este README.
2. Verifica que estén disponibles el host, puerto, base de datos y esquema PostgreSQL. La creación o migración del esquema debe coordinarse con el proyecto de base de datos; iniciar el backend no reemplaza ese proceso.
3. Desde la raíz del repositorio del backend, ejecuta:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Para empaquetar la aplicación:

```powershell
.\mvnw.cmd package
```

El puerto HTTP predeterminado es 8081. Si no se define `SPRING_PROFILES_ACTIVE`, Spring utiliza la configuración base de `application.properties`. El inicio local también requiere un secreto JWT de al menos 32 bytes y `BREVO_API_KEY`, aunque no se use la recuperación de contraseña.

## Configuración

La configuración se obtiene de las propiedades Spring y de las variables de entorno. `.env.example` sirve como referencia; Spring Boot no carga automáticamente un archivo `.env`. Docker Compose puede leerlo para sustituir variables, pero el contenedor solo recibe las que el servicio declara explícitamente.

| Variable | Uso | Requisito o comportamiento actual |
| --- | --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL | Necesarias para una conexión utilizable; los valores predeterminados varían según el perfil |
| `JWT_SECRET` | Firma HMAC de JWT | Obligatoria; mínimo 32 bytes |
| `APP_FRONTEND_URL`, `APP_CORS_ALLOWED_ORIGINS` | URL del frontend y orígenes CORS autorizados | Requeridas en producción |
| `ADMIN_USER_IDS` | UUID de usuarios administradores autorizados, separados por comas | La validación de preparación para producción espera exactamente dos administradores activos autorizados |
| `BREVO_API_KEY` | API HTTP de Brevo para correos de recuperación de contraseña | Requerida por el constructor actual de `EmailService`; falta en `.env.example` y en las variables transferidas por Compose |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Envío SMTP de correo para notificaciones | Configúralas si se habilita el envío de correo; las propiedades de producción requieren credenciales SMTP |
| `WOMPI_ENVIRONMENT` | Entorno Wompi (`sandbox` o `production`) | Defínelo explícitamente en despliegues; fuera de producción suele usar sandbox |
| `WOMPI_PUBLIC_KEY`, `WOMPI_PRIVATE_KEY` | Credenciales Wompi para checkout y API del servidor | Necesarias para las operaciones de pago correspondientes |
| `WOMPI_INTEGRITY_SECRET` | Firma de integridad del checkout | Necesario para firmar referencias e importes |
| `WOMPI_EVENTS_SECRET` | Verificación del checksum de webhooks | Necesario para autenticar eventos del proveedor |
| `PORT`, `SERVER_PORT` | Puerto de escucha (depende del perfil) | Opcionales; base: 8081, producción: 10000 |
| `NOTIFICATIONS_EMAIL_ENABLED`, `NOTIFICATIONS_PUSH_ENABLED` | Activación de canales de notificación | Falso de forma predeterminada |
| `NOTIFICATIONS_WORKER_DELAY_MS` | Intervalo del proceso programado de notificaciones | 30000 ms de forma predeterminada |

No publiques credenciales reales, secretos JWT, contraseñas de base de datos ni llaves de proveedores en incidencias, registros o control de versiones. Usa los mecanismos protegidos de variables de entorno de cada plataforma.

## Referencia de la API

Las rutas de las tablas son relativas a `/api`, excepto las de salud. Salvo que se indique otra cosa, las rutas protegidas requieren un JWT Bearer. Los campos y validaciones exactos se definen en los DTO de solicitud; las tablas indican las rutas y parámetros destacados, no todos los campos de cada DTO.

### Usuarios y autenticación

| Método | Ruta | Acceso | Propósito / respuesta |
| --- | --- | --- | --- |
| `POST` | `/users/register` | Público | Registrar usuario; `201` |
| `POST` | `/users/login` | Público | Autenticar y devolver JWT; `200` |
| `GET` | `/users/me` | Autenticado | Consultar perfil del usuario actual |
| `GET` | `/users/profile` | Autenticado | Consultar perfil del usuario actual |
| `PUT` | `/users/profile` | Autenticado | Actualizar perfil del usuario actual |
| `PUT` | `/users/password` | Autenticado | Cambiar contraseña; `204` |
| `PUT` | `/users/email` | Autenticado | Cambiar correo electrónico |
| `POST` | `/auth/forgot-password` | Público | Solicitar recuperación; la respuesta no revela si existe la cuenta |
| `POST` | `/auth/reset-password` | Público | Restablecer contraseña con el token de recuperación |

### Marcas, servicios y vehículos

| Método | Ruta | Acceso | Parámetros / propósito |
| --- | --- | --- | --- |
| `POST` | `/marcas` | `ADMIN` | Crear marca |
| `GET` | `/marcas/activas` | Autenticado | Listar marcas activas |
| `GET` | `/marcas` | `ADMIN` | Listar marcas |
| `GET` | `/marcas/pendientes` | `ADMIN` | Listar marcas pendientes |
| `GET` | `/marcas/buscar?nombre=...` | `ADMIN` | Buscar marcas |
| `GET` | `/marcas/{id}` | `ADMIN` | Consultar marca |
| `PUT` | `/marcas/{id}` | `ADMIN` | Actualizar marca |
| `PATCH` | `/marcas/{id}/estado?activo=...` | `ADMIN` | Cambiar estado activo |
| `GET` | `/servicios` | Autenticado | Listar servicios |
| `GET` | `/servicios/disponibles` | Autenticado | Listar servicios disponibles |
| `GET` | `/servicios/buscar?nombre=...` | Autenticado | Buscar servicios |
| `GET` | `/servicios/{id}` | Autenticado | Consultar servicio |
| `POST` | `/servicios` | `ADMIN` | Crear servicio |
| `PUT` | `/servicios/{id}` | `ADMIN` | Actualizar servicio |
| `PATCH` | `/servicios/{id}/estado?activo=...` | `ADMIN` | Cambiar estado activo |
| `POST` | `/vehiculos` | Autenticado | Crear vehículo para el usuario actual |
| `GET` | `/vehiculos/mis-vehiculos` | Autenticado | Listar vehículos del usuario actual |
| `GET` | `/vehiculos` | `ADMIN` | Consultar vehículos administrativamente |
| `GET` | `/vehiculos/{id}` | Autenticado | Consultar vehículo, sujeto a comprobación de propiedad o administración |
| `PUT` | `/vehiculos/{id}` | Autenticado | Actualizar vehículo, sujeto a comprobación de propiedad o administración |
| `PATCH` | `/vehiculos/{id}/estado?activo=...` | Autenticado | Cambiar estado, sujeto a comprobación de propiedad o administración |

### Reservas, asignaciones y operadores

| Método | Ruta | Acceso | Parámetros / propósito |
| --- | --- | --- | --- |
| `POST` | `/reservas` | Autenticado | Crear reserva; `201` |
| `GET` | `/reservas` | `ADMIN` | Listado administrativo de reservas |
| `GET` | `/reservas/{id}` | Autenticado | Consultar, sujeto a comprobación de propietario o administrador |
| `GET` | `/reservas/usuario/{id}` | Autenticado | Consultar reservas, sujeto a comprobación de propietario o administrador |
| `PATCH` | `/reservas/{id}/cancelar` | Solo `USER` | Cancelación solicitada por el propietario; ver [cancelación](#cancelación-y-aceptación-de-la-política) |
| `PATCH` | `/reservas/{id}/estado?estado=...` | `ADMIN` | Cambiar estado administrativamente; se rechaza `CANCELADA` |
| `POST` | `/asignaciones` | `ADMIN` | Crear asignación; `201` |
| `GET` | `/asignaciones/mis-asignaciones` | `OPERATOR` | Listar asignaciones del operador actual |
| `PATCH` | `/asignaciones/{id}/estado` | `OPERATOR` | Actualizar estado de una asignación propia; no puede cancelar la reserva |
| `POST` | `/operadores` | `ADMIN` | Crear operador; `201` |
| `GET` | `/operadores` | `ADMIN` | Listar operadores |
| `PATCH` | `/operadores/{id}/estado` | `ADMIN` | Cambiar estado del operador |
| `PATCH` | `/operadores/desactivar-todos` | `ADMIN` | Desactivar operadores |

### Pagos

| Método | Ruta | Acceso | Parámetros / propósito |
| --- | --- | --- | --- |
| `POST` | `/pagos/reserva/{idReserva}` | Autenticado | Iniciar o reutilizar un pago; el servicio verifica propietario/administrador; `201` nuevo o `200` reutilizado |
| `GET` | `/pagos/reserva/{idReserva}` | Autenticado | Consultar pago, sujeto a comprobación de propietario o administrador |
| `POST` | `/pagos/verificar` | Autenticado | Verificar transacción; JSON con `referencia` y `transactionId` |
| `POST` | `/pagos/reconciliar` | `ADMIN` | Conciliar pagos pendientes con el proveedor |
| `POST` | `/pagos/webhook` | Callback público | Evento Wompi; requiere `X-Event-Checksum`; el servidor verifica firma y datos de transacción |

### Notificaciones, calificaciones, auditoría y errores

| Método | Ruta | Acceso | Parámetros / propósito |
| --- | --- | --- | --- |
| `GET` | `/notificaciones?pagina=0&tamano=20&soloNoLeidas=false` | Autenticado | Notificaciones paginadas del usuario actual |
| `GET` | `/notificaciones/no-leidas/count` | Autenticado | Cantidad de notificaciones no leídas |
| `PATCH` | `/notificaciones/{id}/leida` | Autenticado | Marcar como leída una notificación propia |
| `PATCH` | `/notificaciones/leidas` | Autenticado | Marcar como leídas las notificaciones del usuario actual |
| `PUT` | `/notificaciones/dispositivos` | Autenticado | Registrar/actualizar dispositivo; JSON con `instalacionId` y `token` |
| `DELETE` | `/notificaciones/dispositivos/{instalacionId}` | Autenticado | Desactivar token propio; `204` |
| `POST` | `/calificaciones` | Autenticado | Enviar calificación, sujeto a comprobaciones de reserva y propiedad |
| `GET` | `/calificaciones/reserva/{idReserva}` | Autenticado | Consultar calificaciones de una reserva |
| `GET` | `/auditoria` | `ADMIN` | Consultar registros de auditoría con filtros/paginación |
| `GET` | `/log-errores?resuelto=...` | `ADMIN` | Listar registros de errores |
| `PATCH` | `/log-errores/{id}/resolver` | `ADMIN` | Marcar registro como resuelto |

### Endpoints de salud

| Método | Ruta | Acceso | Propósito |
| --- | --- | --- | --- |
| `GET` | `/healthz` | Público | Verificación básica del proceso |
| `GET` | `/actuator/health/**` | Público | Salud de Spring Actuator y sondas configuradas; detalles ocultos en producción |

## Autenticación y autorización

- Envía JWT mediante `Authorization: Bearer <token>`.
- Los tokens usan HMAC-SHA256 (HS256), vencen después de una hora y requieren una clave de firma de al menos 32 bytes.
- Los roles son `USER`, `ADMIN` y `OPERATOR`. El rol y el estado activo se consultan en la cuenta durante la autenticación; el rol guardado en un token antiguo no es la fuente de autoridad.
- La cancelación de reservas tiene acceso más restringido que la autenticación general: solo `USER` puede invocar la ruta y el servicio verifica que la reserva pertenezca a ese usuario.
- La autorización `ADMIN` también está limitada por la lista de UUID configurados y las comprobaciones de preparación para producción.
- Los orígenes CORS se configuran explícitamente con `APP_CORS_ALLOWED_ORIGINS`. Los métodos configurados incluyen `GET`, `POST`, `PUT`, `PATCH`, `DELETE` y `OPTIONS`; los encabezados permitidos incluyen `Authorization`, `Content-Type` y `Accept`. No se habilitan credenciales entre orígenes.
- OpenAPI/Swagger está disponible en desarrollo/local y deshabilitado o denegado en producción.

Estos controles no sustituyen las comprobaciones de propiedad en los servicios. Tener un rol válido no da acceso a las reservas, vehículos, pagos, notificaciones o asignaciones de otra persona.

## Ciclo de vida de una reserva

Los estados persistidos son `PENDIENTE`, `ASIGNADA`, `EN_PROCESO`, `FINALIZADA` y `CANCELADA`. El flujo operativo habitual es:

```text
PENDIENTE -> ASIGNADA -> EN_PROCESO -> FINALIZADA
     \-> CANCELADA (solicitud del propietario, sujeta a validación de pagos)
```

`FINALIZADA` y `CANCELADA` son estados finales para cambios normales. El endpoint administrativo de estado rechaza `CANCELADA`; el endpoint de asignaciones tampoco puede cancelar indirectamente una reserva. Se conservan los valores históricos de los enums y sus registros.

## Cancelación y aceptación de la política

`PATCH /api/reservas/{id}/cancelar` acepta opcionalmente un cuerpo JSON para mantener compatibilidad con clientes anteriores:

```json
{
  "policyAccepted": true,
  "policyVersion": "2026-10-26"
}
```

Los clientes antiguos pueden omitir ambos campos. Si se envían, deben indicar una aceptación explícita válida: `policyAccepted` debe ser `true` y `policyVersion` no puede estar vacía (máximo 50 caracteres). El backend registra esa aceptación afirmativa en la tabla de auditoría existente, con la reserva, versión de política, usuario, fecha y hora, e IP de origen. No inventa una aceptación cuando la solicitud no la expresa afirmativamente.

Reglas de cancelación:

| Condición de la reserva/pago | Resultado |
| --- | --- |
| El solicitante no tiene rol `USER` o no es propietario | Se deniega (respuesta `403` o comportamiento de no encontrado según la consulta de propiedad) |
| La reserva ya está `CANCELADA` y quien solicita es su propietario | Éxito idempotente; no se vuelve a cancelar |
| La reserva no está `PENDIENTE` | `409`, código `RESERVA_NO_PENDIENTE` |
| Hay un pago aprobado | `409`, código `PAGO_APROBADO` |
| Hay un pago/intento pendiente o sin rechazo terminal confirmado por el proveedor | `409`, código `PAGO_PENDIENTE` |
| No hay pagos, o todos los intentos terminaron en rechazo confirmado por el proveedor | Se permite cancelar |

No existe una ruta general para eliminar físicamente reservas. Los cambios administrativos de estado y de asignación no pueden eludir la regla de cancelación por el propietario. Si llega una aprobación de pago después de cancelar, se conserva en el historial de pagos y se genera una alerta administrativa; cancelar no borra la evidencia de Wompi. El backend no ejecuta devoluciones automáticas. Esto no implica renunciar a derechos legales del consumidor como retracto, devolución, reclamación o reversión del pago.

## Pagos con Wompi

El módulo de pagos implementa el inicio, la verificación y conciliación de transacciones, y los webhooks. El backend firma las referencias e importes del checkout con el secreto de integridad configurado. Verifica los checksums de eventos con el secreto correspondiente y valida la identidad de la transacción, referencia, importe, moneda y entorno configurado antes de actualizar los registros.

Los estados de pagos e intentos incluyen pendiente, aprobado, rechazado y una marca de intento aprobado duplicado. Un rechazo confirmado por el proveedor es terminal para determinar si se permite cancelar; un intento inexistente, pendiente o sin confirmación no lo es. Las aprobaciones tardías o duplicadas quedan registradas para conservar trazabilidad. El procesamiento repetido de webhooks es idempotente.

El inicio de pago, la verificación/conciliación, el procesamiento del webhook y la cancelación bloquean los registros de reserva, pago e intento siguiendo un orden consistente que comienza por la reserva. Esto serializa decisiones críticas y evita que una cancelación se adelante a un pago activo o aprobado. El backend no implementa devoluciones automáticas; cualquier devolución o reversión debe seguir un proceso autorizado y conforme a la ley.

Usa credenciales sandbox en pruebas y credenciales de producción únicamente en el entorno productivo. Este README no incluye ni recomienda publicar credenciales reales.

## Notificaciones

La API almacena notificaciones internas y ofrece rutas para listar, consultar no leídas, cambiar su estado y administrar tokens de dispositivos. Un proceso programado gestiona las entregas (intervalo predeterminado: 30 segundos); los canales de correo y notificaciones push están desactivados inicialmente. Cuando se habilita correo para notificaciones se usa SMTP. Las notificaciones push usan el servicio Expo y la consulta de recibos. La recuperación de contraseña utiliza por separado la API de Brevo; por eso `BREVO_API_KEY` es necesaria además de SMTP.

La entrega depende de la configuración y disponibilidad de proveedores externos. Las pruebas locales o con mocks no demuestran que un correo o una notificación push haya llegado a un dispositivo real.

## Base de datos y administración del esquema

El backend se conecta a PostgreSQL mediante Spring Data JPA e Hibernate. Hibernate usa `ddl-auto=validate`: valida los mapeos contra un esquema existente, pero no crea ni actualiza tablas. Las migraciones son responsabilidad del repositorio independiente `lavarapido-base-datos`, que documenta PostgreSQL 16 y Liquibase 5.0.2. Coordina con ese proyecto los cambios y la ejecución de migraciones; no uses el inicio del backend como mecanismo de migración.

## Perfiles, Docker y despliegue

Hay configuraciones base y variantes `dev`, `docker` y `prod`. No se encontró un perfil específico de QA. El perfil de producción escucha por defecto en el puerto 10000, se enlaza a `0.0.0.0`, deshabilita Swagger/OpenAPI, oculta detalles de Actuator, habilita sondas de disponibilidad y requiere configuración específica de base de datos, JWT, CORS, administradores, correo y Wompi.

El `Dockerfile` raíz usa imágenes Eclipse Temurin Java 17, ejecuta pruebas y empaquetado Maven en la etapa de compilación y genera una imagen de ejecución JRE. `docker-compose.yml` define únicamente el servicio backend y espera la red externa `lavarapido-base-datos_default`; no crea PostgreSQL. El puerto del contenedor es 8081 de forma predeterminada. Revisa las variables transferidas antes de usarlo: actualmente no incluye `BREVO_API_KEY`, aunque la aplicación la requiere.

Este repositorio no incluye un manifiesto de despliegue específico de Render. El perfil de producción espera una base PostgreSQL administrada externamente y variables de entorno explícitas. El backend no ejecuta migraciones durante el despliegue. Antes de dirigir tráfico, verifica la salud y la configuración de proveedores en el entorno destino.

## Verificaciones de salud y observabilidad

- `/healthz` es una ruta pública y ligera para comprobar el proceso.
- En producción están configurados Actuator y las sondas de disponibilidad; los detalles de salud se mantienen ocultos.
- La documentación de API y la exposición de datos de salud varían según el perfil; no publiques diagnósticos de desarrollo.
- Un informe operativo anterior, fechado el 2026-10-26, indicó `/healthz` y las sondas de disponibilidad como UP, y `/actuator/health` como DOWN/503. Es información histórica reportada, no una comprobación en vivo realizada al preparar este README; vuelve a verificar el entorno desplegado antes de basarte en ella.

## Errores

Las excepciones de negocio/API generalmente devuelven un objeto JSON con el mensaje `error`. Los conflictos de cancelación incluyen un `code` legible por máquina:

| Estado HTTP | Significado | Código relevante para cancelación |
| --- | --- | --- |
| `400` | Cuerpo, parámetro o transición inválidos | Aceptación de política/transición inválida |
| `401` | Falta autenticación o no es válida | — |
| `403` | El usuario autenticado no tiene permiso por rol o propiedad | — |
| `404` | Recurso inexistente (algunas consultas de propiedad no revelan recursos ajenos) | — |
| `409` | Conflicto de negocio | `PAGO_PENDIENTE`, `PAGO_APROBADO`, `RESERVA_NO_PENDIENTE` |
| `422` | Evento o datos de transacción del proveedor inválidos | Validación de Wompi |
| `502` | Error del proveedor de pagos | Wompi no disponible/error |
| `503` | Dependencia no disponible | Base de datos o servicio externo configurado |
| `500` | Error inesperado o configuración faltante del servidor | — |

Los errores del filtro de autenticación pueden generarse mediante el manejo de errores de Spring Security y no mediante el manejador de excepciones de la aplicación. Por ello, los clientes no deben asumir que todos los errores de seguridad tienen el mismo formato JSON que los errores de negocio.

## Pruebas y limitaciones

Ejecuta las pruebas automatizadas disponibles con:

```powershell
.\mvnw.cmd test
```

La última ejecución local registrada para esta documentación terminó con 194 pruebas: cero fallidas, cero errores y cero omitidas. La suite incluye pruebas unitarias y pruebas de seguridad/controladores con Spring y MockMvc; usa mocks para repositorios y/o interacciones externas de pagos cuando corresponde. No demuestra una integración en vivo con PostgreSQL, Wompi, correo, notificaciones push ni un flujo completo desplegado.

Limitaciones operativas actuales:

- `BREVO_API_KEY` es necesaria al construir la aplicación, pero falta en `.env.example` y en el mapeo de variables de Compose.
- El ciclo de vida del esquema de base de datos es externo y este repositorio no ejecuta migraciones de Hibernate.
- Las pruebas automatizadas de Wompi, SMTP, Brevo y Expo no demuestran disponibilidad ni entregas reales de esos proveedores.
- Cancelar no equivale a devolver dinero. Las aprobaciones tardías o duplicadas quedan trazables y requieren una respuesta adecuada de soporte y cumplimiento.
- En la revisión documental no se encontró una guía de contribución específica ni una política obligatoria de formato/análisis estático. Sigue las prácticas existentes de pruebas y revisión, y evita guardar secretos en el repositorio.
