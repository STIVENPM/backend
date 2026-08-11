# Contexto técnico real — Lava Rápido Vehicular backend

> Auditoría hecha sobre el árbol de trabajo, no sobre una especificación. Fecha de inspección: 2026-07-29.

## Identidad, alcance y estado Git

Este backend está en `C:\proyecto_lavarapido\backend\backend-vehicular`. El repositorio Git raíz es `C:\proyecto_lavarapido\backend`; remoto `origin`: `https://github.com/STIVENPM/backend.git`. La rama activa es `dev-stiven`.

Último commit: `0f0e517bf78feb43c55d5146ee12918129f34b54` — `feat(brands): add approval status and requester relationship` (2026-07-22 18:31:57 -05:00). Los dos anteriores son `3b640ba feat: implement Servicios module with full CRUD and CORS fix for PATCH` y `fa72f40 refactor: migración arquitectura By Module + documentación`.

No existe en las rutas locales comprobables `C:\lavadero-app\backend` ni `C:\proyecto_lavarapido\lavadero-app\backend`; por tanto no es posible demostrar una identidad Git con aquel checkout. **No es el mismo path de trabajo.** Este árbol es claramente más avanzado que la versión descrita: tiene módulo Marca, `SecurityConfig`, `JwtService`, filtro JWT, roles y autorización. Hay cambios locales sin commit en `VehiculoRequestDTO.java`, `VehiculoResponseDTO.java` y `VehiculoService.java` (47 inserciones/56 eliminaciones); este documento describe esos archivos tal como están ahora, no sólo `HEAD`.

## 1. Descripción general

- Maven/Spring Boot: `spring-boot-starter-parent` **4.0.5**; Java **17**. Dependencias: Web MVC, JPA, Security, Validation, Mail, PostgreSQL, Lombok, springdoc 3.0.2 y JJWT 0.12.6.
- Puerto: `server.port=8081`. No hay `application.yml` ni `server.servlet.context-path`: el prefijo de cada API viene de los controllers (`/api/...`).
- PostgreSQL configurado: `jdbc:postgresql://localhost:5432/LavaRapido_Vehicular`; `spring.jpa.hibernate.ddl-auto=validate`. `validate` **no crea DDL**.
- CORS permite `http://localhost:5173` y los métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

## 2. Árbol completo de `src/main/java/com/lavarapido/backend_vehicular`

```text
BackendVehicularApplication.java
auth/{controller/PasswordResetController.java,dto/LoginDTO.java,dto/LoginResponseDTO.java,entity/TokenRecuperacion.java,repository/TokenRecuperacionRepository.java,service/EmailService.java,service/PasswordResetService.java}
marcas/{controller/MarcaController.java,dto/MarcaRequestDTO.java,dto/MarcaResponseDTO.java,entity/Marca.java,repository/MarcaRepository.java,service/MarcaService.java}
roles/entity/Role.java
security/{JwtAuthenticationFilter.java,JwtService.java}
servicios/{controller/ServicioController.java,dto/ServicioRequestDTO.java,dto/ServicioResponseDTO.java,entity/Servicio.java,repository/ServicioRepository.java,service/ServicioService.java}
shared/{config/SecurityConfig.java,config/WebConfig.java,enums/DocumentType.java}
users/{controller/UserController.java,dto/UserRegistrationDTO.java,entity/User.java,entity/UserRole.java,entity/UserRoleId.java,repository/UserRepository.java,repository/UserRoleRepository.java,service/UserService.java}
vehiculos/{controller/VehiculoController.java,dto/VehiculoRequestDTO.java,dto/VehiculoResponseDTO.java,entity/Vehiculo.java,enums/TipoVehiculo.java,repository/VehiculoRepository.java,service/VehiculoService.java}
```

No existen paquetes/entidades/controllers de `Reserva`, `Turno`, `Operador`, `Pago` ni `Calificacion`.

## 3. Entidades JPA reales

### Vehiculo — `vehiculos`

```java
@Entity @Table(name = "vehiculos") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehiculo {
 @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "id_vehiculo") private UUID idVehiculo;
 @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fk_id_usuario", nullable = false) private User usuario;
 @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fk_id_marca", nullable = false) private Marca marca;
 @Column(name = "placa", length = 7, nullable = false, unique = true) private String placa;
 @Column(name = "color", length = 30) private String color;
 @Enumerated(EnumType.STRING) @Column(name = "tipo_vehiculo", length = 10, nullable = false) private TipoVehiculo tipoVehiculo;
 @Column(name = "estado", nullable = false) @Builder.Default private Boolean estado = true;
 @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
 @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
```

### Marca — `marcas`

```java
@Entity @Table(name = "marcas") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Marca {
 @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "id_marca") private UUID idMarca;
 @Column(name = "nombre", length = 30, nullable = false, unique = true) private String nombre;
 @Column(name = "estado", nullable = false) @Builder.Default private Boolean estado = true;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fk_id_usuario_solicitante") private User usuarioSolicitante;
 @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
 @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
```

### User — `users`

```java
@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
 @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "user_id", updatable = false, nullable = false) private UUID userId;
 @Column(name = "email", length = 100, nullable = false, unique = true) private String email;
 @Column(name = "first_name", length = 50, nullable = false) private String firstName;
 @Column(name = "last_name", length = 50) private String lastName;
 @Column(name = "phone_number", length = 10, nullable = false) private String phoneNumber;
 @Enumerated(EnumType.STRING) @Column(name = "document_type", length = 10, nullable = false) private DocumentType documentType;
 @Column(name = "document_number", length = 12, nullable = false) private String documentNumber;
 @Column(name = "password", length = 60, nullable = false) private String password;
 @Column(name = "profile_picture", length = 20, nullable = false) private String profilePicture = "avatar_1";
 @Column(nullable = false) private Boolean status = true;
 @Column(name = "created_at", updatable = false, insertable = false) private LocalDateTime createdAt;
 @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
```

### Roles — `roles` y `user_roles`

```java
@Entity @Table(name = "roles") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Role {
 @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "role_id") private UUID roleId;
 @Column(name = "role_name", length = 20, nullable = false, unique = true) private String roleName;
 @Column(name = "description", length = 255) private String description;
}
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserRoleId implements Serializable {
 @Column(name = "fk_user_id") private UUID userId;
 @Column(name = "fk_role_id") private UUID roleId;
}
@Entity @Table(name = "user_roles") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserRole {
 @EmbeddedId private UserRoleId id;
 @ManyToOne @MapsId("userId") @JoinColumn(name = "fk_user_id") private User user;
 @ManyToOne @MapsId("roleId") @JoinColumn(name = "fk_role_id") private Role role;
 @Column(nullable = false) private Boolean status = true;
 @Column(name = "assigned_at", insertable = false, updatable = false) private LocalDateTime assignedAt;
 @Column(name = "revoked_at") private LocalDateTime revokedAt;
 @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
 @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
```

### Servicio — `servicios`

```java
@Entity @Table(name = "servicios") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Servicio {
 @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "id_servicio") private UUID idServicio;
 @Column(name = "nombre", nullable = false, unique = true, length = 100) private String nombre;
 @Column(name = "descripcion", length = 300) private String descripcion;
 @Column(name = "precio", nullable = false, precision = 10, scale = 0) private BigDecimal precio;
 @Column(name = "duracion_minutos", nullable = false) private Integer duracionMinutos;
 @Column(name = "estado", nullable = false) private Boolean estado = true;
 @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
 @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
```

También existe `TokenRecuperacion`/tabla `tokens_recuperacion`: UUID `id_token`; `@ManyToOne @JoinColumn(fk_id_usuario, nullable=false) User usuario`; `token_hash` String(64), `expiracion_at` LocalDateTime, `usado` Boolean=false, `ip_solicitante` String(45), `created_at` LocalDateTime. No hay entidades de los seis módulos ausentes arriba.

## 4. DTOs y records reales (contrato literal)

```java
public record VehiculoRequestDTO(@NotNull @Pattern(regexp = "^[A-Za-z]{3}[0-9]{3}$|^[A-Za-z]{3}[0-9]{2}[A-Za-z]{1}$") String placa, @Size(max = 30) String color, @NotNull TipoVehiculo tipoVehiculo, @NotNull UUID fkIdMarca) {}
public record VehiculoResponseDTO(UUID idVehiculo, UUID userId, String nombreUsuario, String emailUsuario, UUID idMarca, String nombreMarca, Boolean marcaAprobada, String placa, String color, TipoVehiculo tipoVehiculo, Boolean estado, LocalDateTime createdAt) {}
public record MarcaRequestDTO(@NotBlank @Size(max = 30) String nombre) {}
public record MarcaResponseDTO(UUID idMarca, String nombre, Boolean estado, UUID idUsuarioSolicitante, String emailUsuarioSolicitante, LocalDateTime createdAt) {}
```

Las anotaciones anteriores preservan sus atributos de validación reales: los mensajes no se repiten aquí, pero están en los archivos indicados. No hay `modelo`, `anio`, `observaciones`, `descripcion` de Marca, objeto anidado `marca`, ni objeto anidado `propietario` en ningún DTO de vehículo.

```java
public class UserRegistrationDTO { private String email; private String firstName; private String lastName; private String phoneNumber; private DocumentType documentType; private String documentNumber; private String password; }
public class ServicioRequestDTO { private String nombre; private String descripcion; private BigDecimal precio; private Integer duracionMinutos; }
public class ServicioResponseDTO { private UUID idServicio; private String nombre; private String descripcion; private BigDecimal precio; private Integer duracionMinutos; private Boolean estado; private LocalDateTime createdAt; private LocalDateTime updatedAt; }
public class LoginDTO { private String email; private String password; }
public class LoginResponseDTO { private String token; private UserInfoDTO user; public static class UserInfoDTO { private String userId; private String firstName; private String email; private String role; } }
```

## 5. Controllers/endpoints reales

No hay anotaciones `@PreAuthorize` ni `@Secured`; la seguridad es central en `SecurityConfig`.

| Método/ruta | Entrada | Salida/código declarado | Seguridad efectiva |
|---|---|---|---|
| POST `/api/users/register` | `UserRegistrationDTO` | `User` / 200; error 400 String | pública |
| POST `/api/users/login` | `LoginDTO` | `LoginResponseDTO` / 200; error 401 String | pública |
| GET `/api/users/profile` | — | String / 200 | JWT |
| POST `/api/vehiculos` | `VehiculoRequestDTO` | `VehiculoResponseDTO` / 200; error 400 | JWT |
| GET `/api/vehiculos/mis-vehiculos` | — | `List<VehiculoResponseDTO>` / 200 | JWT |
| GET `/api/vehiculos` | — | lista / 200 | `ADMIN` |
| GET `/api/vehiculos/{id: UUID}` | — | DTO / 200, o String / 404 | JWT |
| PUT `/api/vehiculos/{id: UUID}` | DTO | DTO / 200, o String / 400 | JWT + propietario o ADMIN en service |
| PATCH `/api/vehiculos/{id: UUID}/estado?activo={boolean}` | query `activo` | DTO / 200, o String / 400 | JWT + propietario o ADMIN |
| POST `/api/marcas` | `MarcaRequestDTO` | DTO / 200/error 400 | `ADMIN` |
| GET `/api/marcas/activas` | — | lista / 200 | JWT |
| GET `/api/marcas/pendientes`, `/buscar?nombre=` | — | lista / 200 | `ADMIN` |
| GET `/api/marcas/{id: UUID}` | — | DTO / 200/404 | JWT |
| PUT `/api/marcas/{id: UUID}` | DTO | DTO / 200/400 | `ADMIN` |
| PATCH `/api/marcas/{id: UUID}/estado?activo={boolean}` | query | DTO / 200/400 | `ADMIN` |
| `/api/servicios` | CRUD: POST, GET, GET `/{id}`, PUT `/{id}`, PATCH `/{id}/estado?activo=`, DELETE `/{id}` | Servicio DTO; POST=201, DELETE=204 | JWT (sin rol específico) |
| GET `/api/servicios/disponibles`, `/buscar?nombre=` | — | lista / 200 | JWT |
| POST `/api/auth/forgot-password` | `{email}` record interno | String / 200 | pública |
| POST `/api/auth/reset-password` | `{token,nuevaContrasena}` record interno | String / 200 | pública |

## 6. Módulo Marca — confirmación

**Sí existe módulo Marca en este backend.** Existe tabla mapeada `marcas`, entidad `Marca`, controller, service, repository y los dos DTOs. Su repositorio real extiende `JpaRepository<Marca, UUID>` y declara: `existsByNombreIgnoreCase`, `findByNombreIgnoreCase`, `findByEstadoTrue`, `findByEstadoFalse`, `findByNombreContainingIgnoreCase`. El service normaliza el nombre a mayúsculas; POST la crea con `estado=true` y `usuarioSolicitante=null`. No hay endpoint POST para sugerir marcas: el cambio local actual retiró `marcaSugerida` del DTO de vehículo.

## 7. Seguridad y roles

Sí existen `SecurityConfig`, `JwtService` y `JwtAuthenticationFilter`. JWT es stateless, expira en una hora, su `subject` es email y lleva claim `role`; el filtro crea exactamente `new SimpleGrantedAuthority("ROLE_" + role)`. Las rutas públicas son login/registro, reset de contraseña, Swagger y `/error`; el resto requiere JWT según la tabla.

Los nombres de rol no son enum ni constantes. Son strings almacenados en `roles.role_name`; el código reconoce explícitamente sólo `ADMIN` (para `hasRole("ADMIN")` y `validarPropietarioOAdmin`). Si no existe `UserRole` activo, login devuelve y firma `USER`. No hay lógica de asignar un `UserRole` en `registerUser`: el rol debe existir/asignarse fuera de ese endpoint. No hay uso de `OPERATOR` ni entidad Operador.

La lógica `validarPropietarioOAdmin` lee el usuario desde el email autenticado; permite si `vehiculo.usuario.userId == usuarioAutenticado.userId`; si no, consulta `findActiveRoleByUserId` y sólo permite `"ADMIN"`.

## 8. Login/JWT

`POST /api/users/login` devuelve exactamente un objeto con esta forma JSON:

```json
{"token":"<JWT>","user":{"userId":"<UUID como string>","firstName":"...","email":"...","role":"ADMIN o USER u otro valor BD"}}
```

Sí incluye el rol dentro de `user.role`; además el JWT incluye el claim `role`.

## 9. Base de datos / DDL real

No hay archivos `.sql`, directorio de migraciones, Flyway ni Liquibase en el repositorio. Debido a `spring.jpa.hibernate.ddl-auto=validate`, tampoco se genera DDL al iniciar. Por eso **no existe un DDL SQL real versionado para copiar**. La única definición persistida y comprobable es el mapeo JPA transcrito en la sección 3: tablas `users`, `vehiculos`, `marcas`, `roles`, `user_roles`, `servicios` y `tokens_recuperacion`; sus columnas, UUID, nulabilidad, longitudes y FK están allí. Cualquier SQL que se infiera de eso sería una reconstrucción, no el “DDL real” pedido.

## 10. Contraste con el contrato del frontend

| Punto | Estado real |
|---|---|
| `Vehiculo.idVehiculo` `number` vs UUID | **Pendiente/desalineado**: es `UUID` (JSON string). |
| `marca` objeto `{idMarca,nombre,descripcion,estado}` | **Pendiente/desalineado**: devuelve campos planos `idMarca`, `nombreMarca`, `marcaAprobada`; `Marca` no tiene `descripcion`. |
| `modelo`, `anio`, `color`, `observaciones`, `estado` | **Pendiente**: sólo existen `color` y `estado`; existe adicional `placa`, `tipoVehiculo`; no modelo/año/observaciones. |
| propietario `{idUsuario,nombre,email,telefono}` | **Pendiente parcial**: respuesta plana `userId`, `nombreUsuario`, `emailUsuario`; no `telefono`, no objeto, y el ID no se llama `idUsuario`. |
| PATCH `/vehiculos/{id}/estado` | **Resuelto con diferencia**: existe bajo `/api/vehiculos/{UUID}/estado` y exige query `?activo=true|false`. |
| GET/POST/PUT/PATCH `/marcas...` | **Resuelto con diferencias**: existen bajo `/api/marcas`; GET es `/activas`, `/pendientes`, `/buscar`, `/{UUID}`; PATCH es `/{UUID}/estado?activo=`. |

## 11. Pendientes reales para alinear

Para que el backend cumpla el contrato descrito del frontend, hay que decidir una estrategia y ejecutarla completa:

1. Migrar todos los IDs UUID a numéricos **o** adaptar React/TS a aceptar strings UUID; actualmente no son compatibles.
2. Añadir a entidad, request, response, validación y DDL/migración de vehículo: `modelo`, `anio`, `observaciones`; no basta cambiar el frontend.
3. Elegir respuesta anidada y estable: `marca` con la forma requerida (incluida `descripcion`, inexistente hoy) y `propietario` con `idUsuario`, `nombre`, `email`, `telefono`, o cambiar el frontend a los campos planos reales.
4. Si el contrato de Marca exige `descripcion`, añadir columna/migración/DTO/service. Hoy no existe en ninguna capa.
5. Alinear clientes a `/api`, a los UUID y a `activo` query-param en los PATCH; o cambiar los controllers y documentar una versión de API.
6. Crear migraciones Flyway/Liquibase y un DDL versionado. Con `validate`, el esquema requerido no se crea ni queda auditado en el repositorio.
7. Implementar flujo de asignación administrable de roles y una política para varios roles: `findActiveRoleByUserId` devuelve un solo rol sin orden explícito.
8. Implementar Reserva/Turno, Operador, Pago y Calificación si el frontend los consume: no existen en este backend.

Los archivos fuente son la referencia final: [Vehiculo](src/main/java/com/lavarapido/backend_vehicular/vehiculos/entity/Vehiculo.java), [DTO vehículo](src/main/java/com/lavarapido/backend_vehicular/vehiculos/dto/VehiculoResponseDTO.java), [Marca](src/main/java/com/lavarapido/backend_vehicular/marcas/entity/Marca.java), [SecurityConfig](src/main/java/com/lavarapido/backend_vehicular/shared/config/SecurityConfig.java) y [configuración](src/main/resources/application.properties).
