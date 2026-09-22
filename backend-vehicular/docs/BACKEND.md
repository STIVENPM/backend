# Estado estático del backend

Auditoría realizada sobre `src/main/java/com/lavarapido/backend_vehicular`, `pom.xml` y la configuración local versionada. Es una lectura estática: no se cambió código. La ejecución de pruebas no fue posible porque `mvn` no está instalado ni disponible en el `PATH` de este entorno.

## 1. Stack real

| Componente | Evidencia en código | Estado |
|---|---|---|
| Java | `pom.xml` (`java.version`) | **17** |
| Spring Boot | padre `spring-boot-starter-parent` en `pom.xml` | **4.0.5** |
| Web | `spring-boot-starter-webmvc` | versión gestionada por Boot 4.0.5 |
| JPA / Hibernate | `spring-boot-starter-data-jpa`; `application.properties` | versión gestionada por Boot 4.0.5; PostgreSQL dialect |
| Seguridad | `spring-boot-starter-security`; JWT JJWT | Spring gestionado por Boot; **jjwt 0.12.6** |
| Validación | `spring-boot-starter-validation` | versión gestionada por Boot 4.0.5 |
| OpenAPI | `org.springdoc:springdoc-openapi-starter-webmvc-ui` | **3.0.2** |
| PostgreSQL | `org.postgresql:postgresql` en runtime | versión gestionada por Boot 4.0.5 |
| Mail / Lombok / Devtools | starters/dependencias presentes en `pom.xml` | gestionadas por Boot (Lombok opcional) |

No hay dependencia, configuración ni archivos de **Flyway** o **Liquibase** en el `pom.xml` ni en `src/main/resources`. Se mantiene `spring.jpa.hibernate.ddl-auto=validate` en `src/main/resources/application.properties`: el esquema debe existir y ya coincidir con las entidades; esta aplicación no lo crea ni lo migra.

## 2. Entidades JPA existentes

Todas las clases siguientes tienen `@Entity`; la tabla es la de `@Table`. “Sí” en capa significa que existe una clase específica en el paquete correspondiente (no que todas las operaciones CRUD estén expuestas).

| Entidad | Tabla | Repository | Service | Controller |
|---|---|---:|---:|---:|
| `Role` | `roles` | Sí, `roles/repository/RoleRepository` | No específico | No |
| `User` | `users` | Sí, `users/repository/UserRepository` | Sí, `UserService` | Sí, `UserController` |
| `UserRole` | `user_roles` | Sí, `users/repository/UserRoleRepository` | No específico (lo usan `UserService`/`OperadorService`) | No |
| `Marca` | `marcas` | Sí | Sí | Sí |
| `Servicio` | `servicios` | Sí | Sí | Sí |
| `Vehiculo` | `vehiculos` | Sí | Sí | Sí |
| `Reserva` | `reservas` | Sí | Sí | Sí |
| `Pago` | `pagos` | Sí | Sí, `PagoService` | Sí, `PagoController` |
| `Operador` | `operadores` | Sí | Sí | Sí |
| `Asignacion` | `asignaciones` | Sí | Sí | Sí |
| `Calificacion` | `calificaciones` | Sí | Sí | Sí |
| `Auditoria` | `auditoria` | Sí | Sí | Sí, solo consulta |
| `LogError` | `log_errores` | Sí | Sí | Sí, consulta/resolución |
| `TokenRecuperacion` | `tokens_recuperacion` | Sí | Sí, `PasswordResetService` | Sí, mediante `PasswordResetController` |

Confirmaciones solicitadas:

- **Asignacion** es real: `asignaciones/entity/Asignacion.java`, con `@OneToOne` único a `Reserva`, repository, service y controller.
- **Calificacion** es real: `calificaciones/entity/Calificacion.java`, también única por reserva, con las tres capas.
- **Auditoria** es real: `auditoria/entity/Auditoria.java`, repository con `JpaSpecificationExecutor`, service y controller de listado.
- **LogError** es real: `log_errores/entity/LogError.java`, repository con `JpaSpecificationExecutor`, service y controller de listado/resolución.

## 3. Endpoints REST reales

No se encontró ningún `@PreAuthorize` ni `@EnableMethodSecurity`. La autorización indicada abajo proviene enteramente de `shared/config/SecurityConfig.java`; donde dice “regla final” aplica `anyRequest().authenticated()`.

| Método | Ruta completa | Autorización efectiva |
|---|---|---|
| POST | `/api/users/register` | Pública |
| POST | `/api/users/login` | Pública |
| GET | `/api/users/profile` | Autenticada, regla final |
| PUT | `/api/users/profile` | Autenticada, regla final |
| POST | `/api/auth/forgot-password` | Pública |
| POST | `/api/auth/reset-password` | Pública |
| POST | `/api/marcas` | `ADMIN` |
| GET | `/api/marcas/activas` | Autenticada |
| GET | `/api/marcas` | `ADMIN` |
| GET | `/api/marcas/pendientes` | `ADMIN` |
| GET | `/api/marcas/buscar` | `ADMIN` |
| GET | `/api/marcas/{id}` | `ADMIN` (matcher UUID) |
| PUT | `/api/marcas/{id}` | `ADMIN` |
| PATCH | `/api/marcas/{id}/estado` | `ADMIN` |
| POST | `/api/servicios` | `ADMIN` |
| GET | `/api/servicios` | Autenticada (`/api/servicios/**`) |
| GET | `/api/servicios/disponibles` | Autenticada |
| GET | `/api/servicios/buscar` | Autenticada |
| GET | `/api/servicios/{id}` | Autenticada |
| PUT | `/api/servicios/{id}` | `ADMIN` |
| PATCH | `/api/servicios/{id}/estado` | `ADMIN` |
| POST | `/api/vehiculos` | Autenticada |
| GET | `/api/vehiculos/mis-vehiculos` | Autenticada |
| GET | `/api/vehiculos` | `ADMIN` |
| GET | `/api/vehiculos/{id}` | Autenticada |
| PUT | `/api/vehiculos/{id}` | Autenticada |
| PATCH | `/api/vehiculos/{id}/estado` | Autenticada |
| POST | `/api/reservas` | Autenticada |
| GET | `/api/reservas` | `ADMIN` |
| GET | `/api/reservas/{id}` | Autenticada; propiedad se valida en `ReservaService.obtenerPorId` |
| GET | `/api/reservas/usuario/{id}` | Autenticada; propiedad/admin se valida en servicio |
| PATCH | `/api/reservas/{id}/estado` | `ADMIN` |
| PATCH | `/api/reservas/{id}/cancelar` | Autenticada; propiedad/admin se valida en servicio |
| POST | `/api/pagos/reserva/{idReserva}` | Autenticada, regla final; propiedad/admin en `PagoService.iniciar` |
| GET | `/api/pagos/reserva/{idReserva}` | Autenticada, regla final; propiedad/admin en servicio |
| POST | `/api/pagos/webhook` | Pública; firma comprobada en controller/service |
| POST | `/api/operadores` | `ADMIN` |
| GET | `/api/operadores` | `ADMIN` |
| PATCH | `/api/operadores/{id}/estado` | `ADMIN` |
| PATCH | `/api/operadores/desactivar-todos` | `ADMIN` |
| POST | `/api/asignaciones` | `ADMIN` |
| GET | `/api/asignaciones/mis-asignaciones` | `OPERATOR` |
| PATCH | `/api/asignaciones/{id}/estado` | `OPERATOR`; servicio comprueba que sea su asignación |
| POST | `/api/calificaciones` | Autenticada; servicio exige propietario y reserva `FINALIZADA` |
| GET | `/api/calificaciones/reserva/{idReserva}` | Autenticada; servicio exige propietario o `ROLE_ADMIN` |
| GET | `/api/auditoria` | `ADMIN` |
| GET | `/api/log-errores` | `ADMIN` |
| PATCH | `/api/log-errores/{id}/resolver` | `ADMIN` |

## 4. Estado de Wompi y diagnóstico del checksum

Flujo implementado:

1. `PagoController.iniciar` llama a `PagoService.iniciar`. Este exige propietario o admin, reserva `PENDIENTE`, sin pago previo y precio entero positivo COP; persiste un `Pago` pendiente y devuelve referencia, monto en centavos, llave pública y firma de integridad.
2. `WompiSignatureService.crearFirmaIntegridad` calcula `SHA-256(referencia + montoEnCentavos + "COP" + integritySecret)` con UTF-8 y genera hexadecimal minúsculo.
3. `PagoController.webhook` (`POST /api/pagos/webhook`) llama a `WompiSignatureService.firmaWebhookValida(evento, checksum)`. Si devuelve `false`, responde 401; si devuelve `true`, llama `PagoService.procesarEvento` y responde 200.
4. `PagoService.procesarEvento` procesa sólo `transaction.updated`, localiza el pago por `data.transaction.reference`, contrasta monto recibido, e ignora métodos distintos de `NEQUI`. Para `APPROVED` marca pago aprobado y pasa la reserva de `PENDIENTE` a `ASIGNADA`; para `DECLINED`, `VOIDED` o `ERROR` marca pago rechazado.

### Método exacto y cálculo actual

El cálculo/comparación reportado está en `pagos/service/WompiSignatureService.java`, método `firmaWebhookValida(JsonNode evento, String checksumHeader)`:

1. Lee `evento.signature.properties` y toma como checksum recibido primero el header `X-Event-Checksum` no vacío; sólo si no existe usa `evento.signature.checksum`.
2. Rechaza si `properties` no es arreglo, falta checksum o falta `timestamp`.
3. Recorre **en el mismo orden recibido** cada propiedad. Para cada una busca la ruta separada por puntos desde `evento.data` mediante `leerRuta`; concatena `asString()` si el nodo es un valor y `toString()` si es objeto/arreglo. Si la ruta no existe, rechaza.
4. Añade después `evento.timestamp` como texto y al final `wompi.eventsSecret`.
5. `sha256` codifica toda la concatenación en `StandardCharsets.UTF_8`, calcula SHA-256 y lo convierte a hexadecimal `%02x`, siempre minúsculo.
6. Compara los bytes UTF-8 del texto hexadecimal calculado y del recibido con `MessageDigest.isEqual`; no utiliza `String.equals`.

Hallazgos precisos, sin corrección:

- No hay una lista canónica local ni reordenamiento de campos: el orden depende por completo de `signature.properties` que llegó en el evento. Por tanto el código no calcula una secuencia fija como `id + status + amount`; calcula exactamente la secuencia declarada por el payload. Si el emisor usa otra ruta u orden, el checksum no coincidirá.
- La comparación es literal y sensible a mayúsculas/minúsculas. El hash local es minúsculo, pero `recibido` no se normaliza: un checksum hexadecimal equivalente en mayúsculas es rechazado. `MessageDigest.isEqual` sí evita `equals`, pero aquí compara la representación textual hex (UTF-8), no los bytes digest decodificados.
- La codificación de la cadena de entrada es explícitamente UTF-8; no hay dependencia del charset de plataforma. Para nodos no escalares, en cambio, usa la serialización `toString()` de Jackson, de modo que el valor firmado depende de esa forma de serializar JSON.
- Si llegan ambos checksums, un header no vacío tiene precedencia incluso si `signature.checksum` es válido. El código no compara ambos ni registra cuál fue usado.
- `requiredSecret` puede lanzar `IllegalStateException` si no hay secreto. El controller no lo captura; `GlobalExceptionHandler.handleRuntime` lo transforma en 400, no en 401. Es un fallo de configuración que produce una semántica distinta de “firma inválida”.

## 5. Seguridad

- Registro y login **siguen sin `@Valid`**: `UserController.register(@RequestBody UserRegistrationDTO)` y `login(@RequestBody LoginDTO)` no lo tienen. Además, ambos DTO no declaran constraints de Bean Validation (`users/dto/UserRegistrationDTO.java`, `auth/dto/LoginDTO.java`).
- No se añadió `@PreAuthorize` en los controllers nuevos ni en el resto del código; `SecurityConfig` no habilita seguridad por método. Sí se agregaron reglas de URL para auditoría y log de errores: `/api/auditoria/**` y `/api/log-errores/**` requieren `ADMIN`; las calificaciones requieren autenticación.
- Asignaciones se controla por URL (`POST` ADMIN; GET/PATCH OPERATOR) y el service añade comprobación de propiedad para cambiar estado. Calificaciones añade comprobaciones de propiedad/rol en el service.

## 6. Formato de respuesta

Los controllers nuevos (`AsignacionController`, `CalificacionController`, `AuditoriaController`, `LogErrorController`) devuelven DTO/`Page<DTO>` directamente, sin envelope. No capturan `RuntimeException` localmente: los errores de servicio van a `GlobalExceptionHandler`, cuyo formato es `{"error":"..."}`.

No introducen una inconsistencia nueva en ese patrón. La inconsistencia preexistente permanece en controllers antiguos como `UserController`, `MarcaController` y `VehiculoController`, que capturan excepciones localmente y devuelven a menudo texto plano o `ResponseEntity<?>`, en vez de pasar al manejador global.

## 7. Deuda técnica detectada en módulos nuevos

No hay documentación en `docs/` para asignaciones, calificaciones, auditoría o log de errores (sólo existe documentación de pagos), por lo que los siguientes hallazgos no estaban documentados allí:

- **Auditoría y log no se alimentan automáticamente (alto):** `AuditoriaService.registrar` y `LogErrorService.registrar` existen, pero ninguna clase de producción los inyecta ni invoca (búsqueda en `src/main/java`). Los endpoints sólo permiten leer/resolver registros existentes; en el estado actual esos módulos no registran acciones ni excepciones del backend.
- **Cancelar una asignación deja la reserva asignada (alto):** `AsignacionService.cambiarEstado` permite `asignada/en_proceso -> cancelada`, pero sólo sincroniza la reserva para `en_proceso` y `completada`. Tras cancelar, la reserva queda `ASIGNADA`; por la relación única `Asignacion.reserva` y `existsByReserva_IdReserva`, tampoco puede crearse una nueva asignación. Véanse `AsignacionService.cambiarEstado` y `Asignacion.reserva`.
- **Un operador desactivado puede seguir operando sus asignaciones (alto):** `OperadorService.cambiarEstado` y `desactivarTodos` sólo cambian `Operador.estado`; no revocan `UserRole` OPERATOR. `AsignacionService.obtenerOperadorAutenticado` no comprueba `operador.estado`, de modo que un JWT con `ROLE_OPERATOR` aún puede consultar y actualizar asignaciones.
- **Cambios de estado de reserva no se auditan ni se ligan siempre a la asignación:** `PagoService.procesarEvento` puede fijar directamente `Reserva.estado=ASIGNADA` después de aprobar el pago, aunque aún no existe `Asignacion`; `AsignacionService.crear` acepta después esa reserva. El estado por sí solo no representa que haya operador asignado.
- **Sin migraciones con entidades nuevas (alto operacional):** con `ddl-auto=validate`, desplegar estas cuatro entidades exige crear/actualizar manualmente las tablas, FKs, índices y restricciones; de lo contrario el arranque fallará por validación de esquema.

## 8. Checklist final

| Módulo | Estado | Resumen |
|---|---|---|
| roles | 🟡 | Entidad y repository; sin API/capa de servicio propia. |
| users | 🟡 | Registro/login funcionan como endpoints públicos, pero falta validación de entrada y persiste manejo local de errores. |
| marcas | 🟡 | CRUD parcial y reglas de URL presentes; controller conserva respuestas de error no uniformes. |
| servicios | 🟡 | Capas y reglas presentes; no se detectó migración de esquema. |
| vehiculos | 🟡 | Capas y autenticación presentes; controller conserva manejo local de errores. |
| reservas | 🟡 | Flujo y transiciones implementados; estado `ASIGNADA` puede existir sin `Asignacion`. |
| pagos | ❌ | Widget y webhook implementados, pero la validación de checksum tiene sensibilidad de casing, orden dependiente del payload y prioridades no normalizadas; además no hay pruebas ejecutables en este entorno. |
| operadores | 🟡 | Administración sólo ADMIN; desactivar operador no revoca rol ni bloquea su flujo de asignaciones. |
| asignaciones | ❌ | Entidad/capas/roles existen, pero cancelar deja la reserva bloqueada en `ASIGNADA`. |
| calificaciones | ✅ | Entidad/capas, validación DTO, propiedad de reserva y requisito `FINALIZADA` implementados. |
| auditoria | ❌ | Persistencia y consulta ADMIN existen, pero no hay llamadas de producción a `registrar`. |
| log_errores | ❌ | Persistencia/listado/resolución ADMIN existen, pero no hay llamadas de producción a `registrar`. |
