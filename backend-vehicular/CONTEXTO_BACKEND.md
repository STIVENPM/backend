# CONTEXTO BACKEND — Lava Rápido Vehicular

**Documento generado:** 2026-07-22  
**Propósito:** Contexto de transferencia entre sesiones de IA para continuidad del desarrollo del backend.

---

## 1. Descripción general

### Proyecto
- **Nombre:** backend-vehicular
- **Dominio de negocio:** Lava Rápido Vehicular
- **GroupId:** com.lavarapido
- **ArtifactId:** backend-vehicular
- **Versión:** 0.0.1-SNAPSHOT
- **Puerto de ejecución:** 8081

### Stack técnico
- **Lenguaje:** Java 17
- **Framework:** Spring Boot 4.0.5
- **Gestión de dependencias:** Maven
- **Persistencia:** Spring Data JPA
- **Base de datos:** PostgreSQL
- **Seguridad:** Spring Security + JWT (JJWT 0.12.6) + BCrypt
- **Documentación API:** SpringDoc OpenAPI / Swagger UI
- **Correo:** Spring Mail

### Conexión a base de datos
La conexión está definida en [src/main/resources/application.properties](src/main/resources/application.properties):
- URL JDBC: `jdbc:postgresql://localhost:5432/LavaRapido_Vehicular`
- Usuario: `postgres`
- Driver: `org.postgresql.Driver`
- Hibernate: `spring.jpa.hibernate.ddl-auto=validate`

### Paquete base y clase principal
- **Paquete base:** `com.lavarapido.backend_vehicular`
- **Clase principal:** `BackendVehicularApplication`
- **Archivo principal:** [src/main/java/com/lavarapido/backend_vehicular/BackendVehicularApplication.java](src/main/java/com/lavarapido/backend_vehicular/BackendVehicularApplication.java)

---

## 2. Arquitectura

### Tipo de arquitectura
El backend sigue una arquitectura por capas y por módulos de dominio. Cada dominio tiene su propia estructura de carpeta con:
- `controller`: endpoints REST
- `service`: lógica de negocio
- `repository`: acceso a datos
- `entity`: entidades JPA
- `dto`: objetos de entrada/salida

### Árbol actual de carpetas dentro de src/main/java
```text
src/main/java/com/lavarapido/backend_vehicular/
├── BackendVehicularApplication.java
├── auth/
│   ├── controller/
│   │   └── PasswordResetController.java
│   ├── dto/
│   │   ├── LoginDTO.java
│   │   └── LoginResponseDTO.java
│   ├── entity/
│   │   └── TokenRecuperacion.java
│   ├── repository/
│   │   └── TokenRecuperacionRepository.java
│   └── service/
│       ├── EmailService.java
│       └── PasswordResetService.java
├── roles/
│   └── entity/
│       └── Role.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   └── JwtService.java
├── servicios/
│   ├── controller/
│   │   └── ServicioController.java
│   ├── dto/
│   │   ├── ServicioRequestDTO.java
│   │   └── ServicioResponseDTO.java
│   ├── entity/
│   │   └── Servicio.java
│   ├── repository/
│   │   └── ServicioRepository.java
│   └── service/
│       └── ServicioService.java
├── shared/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── WebConfig.java
│   └── enums/
│       └── DocumentType.java
└── users/
    ├── controller/
    │   └── UserController.java
    ├── dto/
    │   └── UserRegistrationDTO.java
    ├── entity/
    │   ├── User.java
    │   ├── UserRole.java
    │   └── UserRoleId.java
    ├── repository/
    │   ├── UserRepository.java
    │   └── UserRoleRepository.java
    └── service/
        └── UserService.java
```

### Función de los módulos principales
- **auth**: recuperación de contraseña, generación de tokens temporales y envío de correos.
- **roles**: definición de roles del sistema.
- **security**: autenticación JWT y filtros de seguridad para validar solicitudes entrantes.
- **servicios**: CRUD de servicios ofrecidos por el negocio.
- **shared**: configuración global y enums reutilizables.
- **users**: registro, login, perfil y asociación de usuarios con roles.

---

## 3. Módulos implementados

### Módulo auth
Archivos:
- [src/main/java/com/lavarapido/backend_vehicular/auth/controller/PasswordResetController.java](src/main/java/com/lavarapido/backend_vehicular/auth/controller/PasswordResetController.java)
- [src/main/java/com/lavarapido/backend_vehicular/auth/service/PasswordResetService.java](src/main/java/com/lavarapido/backend_vehicular/auth/service/PasswordResetService.java)
- [src/main/java/com/lavarapido/backend_vehicular/auth/service/EmailService.java](src/main/java/com/lavarapido/backend_vehicular/auth/service/EmailService.java)
- [src/main/java/com/lavarapido/backend_vehicular/auth/entity/TokenRecuperacion.java](src/main/java/com/lavarapido/backend_vehicular/auth/entity/TokenRecuperacion.java)
- [src/main/java/com/lavarapido/backend_vehicular/auth/repository/TokenRecuperacionRepository.java](src/main/java/com/lavarapido/backend_vehicular/auth/repository/TokenRecuperacionRepository.java)
- [src/main/java/com/lavarapido/backend_vehicular/auth/dto/LoginDTO.java](src/main/java/com/lavarapido/backend_vehicular/auth/dto/LoginDTO.java)
- [src/main/java/com/lavarapido/backend_vehicular/auth/dto/LoginResponseDTO.java](src/main/java/com/lavarapido/backend_vehicular/auth/dto/LoginResponseDTO.java)

Resumen:
- **PasswordResetController** expone los endpoints públicos de recuperación de contraseña. Maneja la solicitud de reset y su confirmación con un token.
- **PasswordResetService** genera un token seguro, lo almacena hasheado, controla expiración y actualiza la contraseña del usuario.
- **TokenRecuperacionRepository** permite buscar un token hash y verificar si ya existe uno activo para un usuario.

Endpoints expuestos:
- `POST /api/auth/forgot-password`: recibe `{ email }` y devuelve un mensaje genérico indicando que si el correo existe se enviará un enlace.
- `POST /api/auth/reset-password`: recibe `{ token, nuevaContrasena }` y devuelve un mensaje de éxito si el token es válido.

### Módulo servicios
Archivos:
- [src/main/java/com/lavarapido/backend_vehicular/servicios/controller/ServicioController.java](src/main/java/com/lavarapido/backend_vehicular/servicios/controller/ServicioController.java)
- [src/main/java/com/lavarapido/backend_vehicular/servicios/service/ServicioService.java](src/main/java/com/lavarapido/backend_vehicular/servicios/service/ServicioService.java)
- [src/main/java/com/lavarapido/backend_vehicular/servicios/entity/Servicio.java](src/main/java/com/lavarapido/backend_vehicular/servicios/entity/Servicio.java)
- [src/main/java/com/lavarapido/backend_vehicular/servicios/repository/ServicioRepository.java](src/main/java/com/lavarapido/backend_vehicular/servicios/repository/ServicioRepository.java)
- [src/main/java/com/lavarapido/backend_vehicular/servicios/dto/ServicioRequestDTO.java](src/main/java/com/lavarapido/backend_vehicular/servicios/dto/ServicioRequestDTO.java)
- [src/main/java/com/lavarapido/backend_vehicular/servicios/dto/ServicioResponseDTO.java](src/main/java/com/lavarapido/backend_vehicular/servicios/dto/ServicioResponseDTO.java)

Resumen:
- **ServicioController** expone el CRUD de servicios y permite listar solo los disponibles o buscarlos por nombre.
- **ServicioService** aplica validaciones de negocio como nombre único y cambios de estado.
- **ServicioRepository** encapsula consultas por nombre, estado activo y búsqueda parcial.

Endpoints expuestos:
- `POST /api/servicios`: crea un servicio con nombre, descripción, precio y duración.
- `GET /api/servicios`: lista todos los servicios.
- `GET /api/servicios/disponibles`: lista solo los servicios activos.
- `GET /api/servicios/buscar?nombre=...`: busca por nombre.
- `GET /api/servicios/{id}`: obtiene un servicio por UUID.
- `PUT /api/servicios/{id}`: actualiza un servicio.
- `PATCH /api/servicios/{id}/estado?activo=true|false`: cambia el estado del servicio.
- `DELETE /api/servicios/{id}`: elimina el recurso (implementado como cambio de estado).

### Módulo users
Archivos:
- [src/main/java/com/lavarapido/backend_vehicular/users/controller/UserController.java](src/main/java/com/lavarapido/backend_vehicular/users/controller/UserController.java)
- [src/main/java/com/lavarapido/backend_vehicular/users/service/UserService.java](src/main/java/com/lavarapido/backend_vehicular/users/service/UserService.java)
- [src/main/java/com/lavarapido/backend_vehicular/users/entity/User.java](src/main/java/com/lavarapido/backend_vehicular/users/entity/User.java)
- [src/main/java/com/lavarapido/backend_vehicular/users/entity/UserRole.java](src/main/java/com/lavarapido/backend_vehicular/users/entity/UserRole.java)
- [src/main/java/com/lavarapido/backend_vehicular/users/entity/UserRoleId.java](src/main/java/com/lavarapido/backend_vehicular/users/entity/UserRoleId.java)
- [src/main/java/com/lavarapido/backend_vehicular/users/repository/UserRepository.java](src/main/java/com/lavarapido/backend_vehicular/users/repository/UserRepository.java)
- [src/main/java/com/lavarapido/backend_vehicular/users/repository/UserRoleRepository.java](src/main/java/com/lavarapido/backend_vehicular/users/repository/UserRoleRepository.java)
- [src/main/java/com/lavarapido/backend_vehicular/users/dto/UserRegistrationDTO.java](src/main/java/com/lavarapido/backend_vehicular/users/dto/UserRegistrationDTO.java)

Resumen:
- **UserController** expone registro y login de usuarios, además de un endpoint de perfil autenticado.
- **UserService** valida unicidad del correo, encripta contraseñas y genera el JWT tras un login exitoso.
- **UserRoleRepository** permite localizar el rol activo del usuario mediante una consulta JPQL.

Endpoints expuestos:
- `POST /api/users/register`: registra un usuario con datos personales y contraseña.
- `POST /api/users/login`: valida credenciales y devuelve un JWT.
- `GET /api/users/profile`: devuelve el email autenticado desde el contexto de seguridad.

### Módulo roles
Archivos:
- [src/main/java/com/lavarapido/backend_vehicular/roles/entity/Role.java](src/main/java/com/lavarapido/backend_vehicular/roles/entity/Role.java)

Resumen:
- **Role** define los roles del sistema, pero por ahora no tiene controller ni service propios.

---

## 4. Entidades y modelos de datos

### User
- **Clase:** `User`
- **Tabla:** `users`
- **Campos principales:**
  - `userId: UUID`
  - `email: String`
  - `firstName: String`
  - `lastName: String`
  - `phoneNumber: String`
  - `documentType: DocumentType`
  - `documentNumber: String`
  - `password: String`
  - `profilePicture: String`
  - `status: Boolean`
  - `createdAt: LocalDateTime`
  - `updatedAt: LocalDateTime`
- **Relaciones:** 1:N con `UserRole` y 1:N con `TokenRecuperacion`.
- **ID:** `@GeneratedValue(strategy = GenerationType.UUID)`.

### Role
- **Clase:** `Role`
- **Tabla:** `roles`
- **Campos principales:**
  - `roleId: UUID`
  - `roleName: String`
  - `description: String`
- **Relaciones:** 1:N con `UserRole`.
- **ID:** UUID generado por JPA.

### UserRole
- **Clase:** `UserRole`
- **Tabla:** `user_roles`
- **Campos principales:**
  - `id: UserRoleId` (clave compuesta)
  - `user: User`
  - `role: Role`
  - `status: Boolean`
  - `assignedAt: LocalDateTime`
  - `revokedAt: LocalDateTime`
- **Relaciones:** muchos a uno hacia `User` y `Role`.
- **ID:** clave compuesta `UserRoleId`.

### UserRoleId
- **Clase:** `UserRoleId`
- **Tabla:** no existe como tabla propia; es una clave embebida para `user_roles`.
- **Campos:** `userId: UUID`, `roleId: UUID`.

### Servicio
- **Clase:** `Servicio`
- **Tabla:** `servicios`
- **Campos principales:**
  - `idServicio: UUID`
  - `nombre: String`
  - `descripcion: String`
  - `precio: BigDecimal`
  - `duracionMinutos: Integer`
  - `estado: Boolean`
  - `createdAt: LocalDateTime`
  - `updatedAt: LocalDateTime`
- **Relaciones:** por ahora sin relaciones con otras entidades.
- **ID:** UUID generado por JPA.

### TokenRecuperacion
- **Clase:** `TokenRecuperacion`
- **Tabla:** `tokens_recuperacion`
- **Campos principales:**
  - `idToken: UUID`
  - `usuario: User`
  - `tokenHash: String`
  - `expiracionAt: LocalDateTime`
  - `usado: Boolean`
  - `ipSolicitante: String`
  - `createdAt: LocalDateTime`
- **Relaciones:** muchos a uno con `User`.
- **ID:** UUID generado por JPA.

---

## 5. Base de datos

### Tablas actualmente mapeadas por JPA
- `users`
- `roles`
- `user_roles`
- `servicios`
- `tokens_recuperacion`

### Columnas principales por tabla
- `users`: `user_id`, `email`, `first_name`, `last_name`, `phone_number`, `document_type`, `document_number`, `password`, `profile_picture`, `status`, `created_at`, `updated_at`
- `roles`: `role_id`, `role_name`, `description`
- `user_roles`: `fk_user_id`, `fk_role_id`, `status`, `assigned_at`, `revoked_at`, `created_at`, `updated_at`
- `servicios`: `id_servicio`, `nombre`, `descripcion`, `precio`, `duracion_minutos`, `estado`, `created_at`, `updated_at`
- `tokens_recuperacion`: `id_token`, `fk_id_usuario`, `token_hash`, `expiracion_at`, `usado`, `ip_solicitante`, `created_at`

### Constraints relevantes
- `users.email` es `unique`.
- `roles.role_name` es `unique`.
- `servicios.nombre` es `unique`.
- `tokens_recuperacion.token_hash` es `unique`.
- No hay `@Check` ni `@OnDelete` declarados explícitamente en el código actual.
- Las relaciones FK se modelan con `@JoinColumn` pero no se ha definido un `ON DELETE` explícito.

### Estrategia de generación de IDs
- Los IDs principales de las entidades se generan con `GenerationType.UUID`.
- El esquema de la base de datos usa `UUID` como tipo principal y el valor es generado por JPA/Hibernate.
- Los campos `created_at` y `updated_at` se dejan a la base de datos para ser completados por defecto.

---

## 6. Configuración y seguridad

### Seguridad activa
- El backend usa Spring Security con un filtro JWT personalizado.
- La configuración de seguridad está en [src/main/java/com/lavarapido/backend_vehicular/shared/config/SecurityConfig.java](src/main/java/com/lavarapido/backend_vehicular/shared/config/SecurityConfig.java).
- CSRF está deshabilitado.
- La gestión de sesiones es stateless.

### CORS
- Configurado en [src/main/java/com/lavarapido/backend_vehicular/shared/config/WebConfig.java](src/main/java/com/lavarapido/backend_vehicular/shared/config/WebConfig.java).
- Orígenes permitidos: `http://localhost:5173` y `http://localhost:8081`.
- Métodos permitidos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.

### Rutas públicas vs protegidas
Rutas públicas:
- `POST /api/users/register`
- `POST /api/users/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- Swagger y OpenAPI (`/swagger-ui/**`, `/v3/api-docs/**`, `/error`)

Rutas protegidas:
- `GET /api/users/profile`
- Todos los endpoints de `/api/servicios`

### Manejo de tokens
- El login genera un JWT con el email como subject.
- El token se valida en [src/main/java/com/lavarapido/backend_vehicular/security/JwtAuthenticationFilter.java](src/main/java/com/lavarapido/backend_vehicular/security/JwtAuthenticationFilter.java).
- La firma y la validación se hacen con `JwtService` usando la clave definida en [src/main/resources/application.properties](src/main/resources/application.properties).
- El token tiene una duración fija de 1 hora.

---

## 7. Convenciones del proyecto

### Convenciones de paquetes y clases
- Los paquetes siguen la forma `com.lavarapido.backend_vehicular.<modulo>.<capa>`.
- Los nombres de clase son consistentes:
  - Controllers: `*Controller`
  - Services: `*Service`
  - Repositories: `*Repository`
  - Entities: `*`
  - DTOs: `*DTO`

### Imports y anotaciones habituales
- Uso de `jakarta.persistence.*` para entidades y mapeo JPA.
- Uso de `jakarta.validation.*` para validaciones en DTOs.
- Uso de Lombok en entidades y servicios para reducir boilerplate.
- Uso de `@RestController`, `@Service`, `@Repository`, `@Entity`, `@Table`, `@Column`, `@GeneratedValue` y `@RequestMapping`.

### Manejo de errores
- El proyecto usa `RuntimeException` con mensajes simples para errores de negocio.
- La validación de DTOs se realiza con annotations como `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`.
- No se observa un manejador global de excepciones centralizado.

### Reglas de negocio transversales
- Las contraseñas se almacenan siempre como hash con BCrypt.
- Los tokens de recuperación se guardan hasheados con SHA-256.
- Los servicios tienen un campo `estado` para simular borrado lógico.
- Los usuarios tienen un campo `status` para activar o desactivar cuentas.
- El proyecto prioriza UUID como identificador principal.

---

## 8. Endpoints totales activos

| Módulo | Método | Ruta | Requiere autenticación |
|---|---|---|---|
| Users | POST | `/api/users/register` | No |
| Users | POST | `/api/users/login` | No |
| Users | GET | `/api/users/profile` | Sí |
| Auth | POST | `/api/auth/forgot-password` | No |
| Auth | POST | `/api/auth/reset-password` | No |
| Servicios | POST | `/api/servicios` | Sí |
| Servicios | GET | `/api/servicios` | Sí |
| Servicios | GET | `/api/servicios/disponibles` | Sí |
| Servicios | GET | `/api/servicios/buscar` | Sí |
| Servicios | GET | `/api/servicios/{id}` | Sí |
| Servicios | PUT | `/api/servicios/{id}` | Sí |
| Servicios | PATCH | `/api/servicios/{id}/estado` | Sí |
| Servicios | DELETE | `/api/servicios/{id}` | Sí |
| Swagger | GET | `/swagger-ui.html` | No |
| Swagger | GET | `/v3/api-docs` | No |

---

## 9. Pendientes y próximo paso

¿Que módulo, tabla o funcionalidad quieres que quede documentada como próximo paso pendiente?
