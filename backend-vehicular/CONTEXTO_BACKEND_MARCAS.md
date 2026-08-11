# CONTEXTO_BACKEND_MARCAS

## SQL relacionada con marcas

No se encontró un bloque SQL con `CREATE TABLE marcas` en el workspace del proyecto.

## Archivos Java

### src/main/java/com/lavarapido/backend_vehicular/marcas/entity/Marca.java
```java
package com.lavarapido.backend_vehicular.marcas.entity;


import com.lavarapido.backend_vehicular.users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marcas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_marca")
    private UUID idMarca;

    @Column(name = "nombre", length = 30, nullable = false, unique = true)
    private String nombre;

    // TRUE = aprobada/visible en el catálogo. FALSE = pendiente de revisión
    // (sugerida por un cliente al registrar un vehículo con marca nueva).
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private Boolean estado = true;

    // Usuario que sugirió la marca (nullable: NULL si la creó el admin
    // directamente, o si el usuario que la sugirió borró su cuenta).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_id_usuario_solicitante")
    private User usuarioSolicitante;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
```

### src/main/java/com/lavarapido/backend_vehicular/marcas/dto/MarcaRequestDTO.java
```java
package com.lavarapido.backend_vehicular.marcas.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcaRequestDTO(

    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
    String nombre

) {}
```

### src/main/java/com/lavarapido/backend_vehicular/marcas/dto/MarcaResponseDTO.java
```java
package com.lavarapido.backend_vehicular.marcas.dto;


import java.time.LocalDateTime;
import java.util.UUID;

public record MarcaResponseDTO(
    UUID idMarca,
    String nombre,
    Boolean estado,
    // Datos de quién la sugirió, útil para el admin al revisar pendientes.
    // Ambos quedan null si la marca fue creada directamente por el admin.
    UUID idUsuarioSolicitante,
    String emailUsuarioSolicitante,
    LocalDateTime createdAt
) {}
```

### src/main/java/com/lavarapido/backend_vehicular/marcas/repository/MarcaRepository.java
```java
package com.lavarapido.backend_vehicular.marcas.repository;


import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarcaRepository extends JpaRepository<Marca, UUID> {

    // Para validar unicidad al crear/sugerir una marca (evita duplicados
    // por mayúsculas/minúsculas, ej. "Toyota" vs "TOYOTA").
    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Marca> findByNombreIgnoreCase(String nombre);

    // Catálogo aprobado — lo que ve el cliente en el <select> al
    // registrar su vehículo.
    List<Marca> findByEstadoTrue();

    // Solicitudes pendientes por revisar — panel del admin.
    List<Marca> findByEstadoFalse();

    // Búsqueda parcial para el admin (ej. barra de búsqueda de marcas).
    List<Marca> findByNombreContainingIgnoreCase(String nombre);
}
```

### src/main/java/com/lavarapido/backend_vehicular/marcas/service/MarcaService.java
```java
package com.lavarapido.backend_vehicular.marcas.service;


import com.lavarapido.backend_vehicular.marcas.dto.MarcaRequestDTO;
import com.lavarapido.backend_vehicular.marcas.dto.MarcaResponseDTO;
import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.marcas.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarcaService {

    private final MarcaRepository marcaRepository;

    // ── CREAR (admin — marca nace ya aprobada) ────────────────────
    @Transactional
    public MarcaResponseDTO crear(MarcaRequestDTO dto) {

        String nombreNormalizado = dto.nombre().trim().toUpperCase();

        if (marcaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new RuntimeException("Ya existe una marca con ese nombre");
        }

        Marca marca = new Marca();
        marca.setNombre(nombreNormalizado);
        marca.setEstado(true); // el admin la crea directamente aprobada
        marca.setUsuarioSolicitante(null);

        Marca guardada = marcaRepository.save(marca);
        return mapearAResponse(guardada);
    }

    // ── LISTAR ACTIVAS (catálogo — cliente + admin) ────────────────
    public List<MarcaResponseDTO> listarActivas() {
        return marcaRepository.findByEstadoTrue()
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── LISTAR PENDIENTES (panel admin — solicitudes por revisar) ──
    public List<MarcaResponseDTO> listarPendientes() {
        return marcaRepository.findByEstadoFalse()
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── BUSCAR (admin — barra de búsqueda del catálogo completo) ───
    public List<MarcaResponseDTO> buscar(String nombre) {
        return marcaRepository.findByNombreContainingIgnoreCase(nombre)
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── OBTENER POR ID ───────────────────────────────────────────────
    public MarcaResponseDTO obtenerPorId(UUID idMarca) {
        Marca marca = buscarMarcaOrThrow(idMarca);
        return mapearAResponse(marca);
    }

    // ── ACTUALIZAR (admin — ej. corregir nombre mal escrito antes de aprobar) ──
    @Transactional
    public MarcaResponseDTO actualizar(UUID idMarca, MarcaRequestDTO dto) {

        Marca marca = buscarMarcaOrThrow(idMarca);
        String nombreNormalizado = dto.nombre().trim().toUpperCase();

        // Si cambió el nombre, valida que no choque con otra marca existente.
        if (!nombreNormalizado.equals(marca.getNombre())
                && marcaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new RuntimeException("Ya existe una marca con ese nombre");
        }

        marca.setNombre(nombreNormalizado);

        Marca actualizada = marcaRepository.save(marca);
        return mapearAResponse(actualizada);
    }

    // ── CAMBIAR ESTADO (aprobar una pendiente / activar-desactivar) ──
    @Transactional
    public MarcaResponseDTO cambiarEstado(UUID idMarca, boolean activo) {
        Marca marca = buscarMarcaOrThrow(idMarca);
        marca.setEstado(activo);
        Marca actualizada = marcaRepository.save(marca);
        return mapearAResponse(actualizada);
    }

    // ── Utilidades privadas ───────────────────────────────────────────

    private Marca buscarMarcaOrThrow(UUID idMarca) {
        return marcaRepository.findById(idMarca)
            .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
    }

    private MarcaResponseDTO mapearAResponse(Marca marca) {
        boolean tieneSolicitante = marca.getUsuarioSolicitante() != null;

        return new MarcaResponseDTO(
            marca.getIdMarca(),
            marca.getNombre(),
            marca.getEstado(),
            tieneSolicitante ? marca.getUsuarioSolicitante().getUserId() : null,
            tieneSolicitante ? marca.getUsuarioSolicitante().getEmail() : null,
            marca.getCreatedAt()
        );
    }
}
```

### src/main/java/com/lavarapido/backend_vehicular/marcas/controller/MarcaController.java
```java
package com.lavarapido.backend_vehicular.marcas.controller;


import com.lavarapido.backend_vehicular.marcas.dto.MarcaRequestDTO;
import com.lavarapido.backend_vehicular.marcas.dto.MarcaResponseDTO;
import com.lavarapido.backend_vehicular.marcas.service.MarcaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/marcas")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService marcaService;

    // ── Crear marca directamente (admin — ya aprobada) ───────────────
    // TODO: restringir a rol ADMIN cuando exista control de roles.
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody MarcaRequestDTO dto) {
        try {
            MarcaResponseDTO creada = marcaService.crear(dto);
            return ResponseEntity.ok(creada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Catálogo aprobado (cliente + admin) ───────────────────────────
    @GetMapping("/activas")
    public ResponseEntity<List<MarcaResponseDTO>> listarActivas() {
        return ResponseEntity.ok(marcaService.listarActivas());
    }

    // ── Solicitudes pendientes de aprobación (admin) ──────────────────
    // TODO: restringir a rol ADMIN cuando exista control de roles.
    @GetMapping("/pendientes")
    public ResponseEntity<List<MarcaResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(marcaService.listarPendientes());
    }

    // ── Búsqueda en el catálogo completo (admin) ──────────────────────
    // TODO: restringir a rol ADMIN cuando exista control de roles.
    @GetMapping("/buscar")
    public ResponseEntity<List<MarcaResponseDTO>> buscar(@RequestParam String nombre) {
        return ResponseEntity.ok(marcaService.buscar(nombre));
    }

    // ── Obtener por id ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(marcaService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ── Actualizar (corregir nombre — admin) ──────────────────────────
    // TODO: restringir a rol ADMIN cuando exista control de roles.
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody MarcaRequestDTO dto) {
        try {
            MarcaResponseDTO actualizada = marcaService.actualizar(id, dto);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Cambiar estado (aprobar pendiente / activar-desactivar — admin) ──
    // TODO: restringir a rol ADMIN cuando exista control de roles.
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        try {
            MarcaResponseDTO actualizada = marcaService.cambiarEstado(id, activo);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
```

### src/main/java/com/lavarapido/backend_vehicular/vehiculos/entity/Vehiculo.java
```java
package com.lavarapido.backend_vehicular.vehiculos.entity;

import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_vehiculo")
    private UUID idVehiculo;

    // Dueño del vehículo. Se asigna siempre desde el usuario autenticado
    // (JWT) en el service — nunca se recibe directamente del body del DTO.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_usuario", nullable = false)
    private User usuario;

    // Referencia al catálogo de marcas. Nunca NULL: si el cliente sugiere
    // una marca nueva, primero se crea en `marcas` (estado = false) y
    // luego el vehículo apunta a ese registro recién creado.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_marca", nullable = false)
    private Marca marca;

    @Column(name = "placa", length = 7, nullable = false, unique = true)
    private String placa;

    @Column(name = "color", length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", length = 10, nullable = false)
    private TipoVehiculo tipoVehiculo;

    @Column(name = "estado", nullable = false)
    @Builder.Default
    private Boolean estado = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
```

### src/main/java/com/lavarapido/backend_vehicular/vehiculos/dto/VehiculoRequestDTO.java
```java
package com.lavarapido.backend_vehicular.vehiculos.dto;


import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehiculoRequestDTO(

    // Placa: acepta formato antiguo (ABC123) o nuevo (ABC12A).
    // Se normaliza a mayúsculas en el service antes de guardar.
    @NotNull(message = "La placa es obligatoria")
    @Pattern(
        regexp = "^[A-Za-z]{3}[0-9]{3}$|^[A-Za-z]{3}[0-9]{2}[A-Za-z]{1}$",
        message = "La placa debe tener el formato ABC123 o ABC12A"
    )
    String placa,

    @Size(max = 30, message = "El color no puede superar los 30 caracteres")
    String color,

    @NotNull(message = "El tipo de vehículo es obligatorio")
    TipoVehiculo tipoVehiculo,

    // ── Marca: SOLO UNO de los dos campos debe venir, nunca ambos ni ninguno ──

    // Caso 1: el cliente elige una marca ya existente del catálogo.
    UUID fkIdMarca,

    // Caso 2: el cliente no encuentra su marca y sugiere una nueva.
    // El service la crea en `marcas` con estado = false (pendiente de
    // aprobación) y usa ese id para el vehículo.
    @Size(max = 30, message = "El nombre de la marca no puede superar los 30 caracteres")
    String marcaSugerida

) {}
```

### src/main/java/com/lavarapido/backend_vehicular/vehiculos/dto/VehiculoResponseDTO.java
```java
package com.lavarapido.backend_vehicular.vehiculos.dto;


import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehiculoResponseDTO(
    UUID idVehiculo,
    UUID userId,
    UUID idMarca,
    String nombreMarca,
    // Informa al frontend si la marca todavía no fue aprobada por el
    // admin — útil para mostrar un aviso tipo "marca en revisión".
    Boolean marcaAprobada,
    String placa,
    String color,
    TipoVehiculo tipoVehiculo,
    Boolean estado,
    LocalDateTime createdAt
) {}
```

### src/main/java/com/lavarapido/backend_vehicular/vehiculos/repository/VehiculoRepository.java
```java
package com.lavarapido.backend_vehicular.vehiculos.repository;

import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface VehiculoRepository extends JpaRepository<Vehiculo, UUID> {

    // Validar unicidad de placa al crear/actualizar.
    boolean existsByPlaca(String placa);

    Optional<Vehiculo> findByPlaca(String placa);

    // Vehículos del usuario autenticado (app móvil — "mis vehículos").
    List<Vehiculo> findByUsuario_UserId(UUID userId);

    // Solo los activos del usuario (borrado lógico aplicado).
    List<Vehiculo> findByUsuario_UserIdAndEstadoTrue(UUID userId);
}
```

### src/main/java/com/lavarapido/backend_vehicular/vehiculos/service/VehiculoService.java
```java
package com.lavarapido.backend_vehicular.vehiculos.service;


import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.marcas.repository.MarcaRepository;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoRequestDTO;
import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoResponseDTO;
import com.lavarapido.backend_vehicular.vehiculos.entity.Vehiculo;
import com.lavarapido.backend_vehicular.vehiculos.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final MarcaRepository marcaRepository;
    private final UserRepository userRepository;

    // ── CREAR ──────────────────────────────────────────────────────
    @Transactional
    public VehiculoResponseDTO crear(VehiculoRequestDTO dto) {

        User usuarioAutenticado = obtenerUsuarioAutenticado();

        // Normaliza la placa a mayúsculas (el CHECK de la BD exige [A-Z]).
        String placaNormalizada = dto.placa().toUpperCase();

        if (vehiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new RuntimeException("Ya existe un vehículo registrado con esa placa");
        }

        Marca marca = resolverMarca(dto.fkIdMarca(), dto.marcaSugerida(), usuarioAutenticado);

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setUsuario(usuarioAutenticado);
        vehiculo.setMarca(marca);
        vehiculo.setPlaca(placaNormalizada);
        vehiculo.setColor(dto.color());
        vehiculo.setTipoVehiculo(dto.tipoVehiculo());
        vehiculo.setEstado(true);

        Vehiculo guardado = vehiculoRepository.save(vehiculo);
        return mapearAResponse(guardado);
    }

    // ── LISTAR "MIS VEHÍCULOS" (app móvil) ────────────────────────
    public List<VehiculoResponseDTO> listarMisVehiculos() {
        User usuarioAutenticado = obtenerUsuarioAutenticado();
        return vehiculoRepository.findByUsuario_UserIdAndEstadoTrue(usuarioAutenticado.getUserId())
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── LISTAR TODOS (panel admin) ────────────────────────────────
    public List<VehiculoResponseDTO> listarTodos() {
        return vehiculoRepository.findAll()
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── OBTENER POR ID ─────────────────────────────────────────────
    public VehiculoResponseDTO obtenerPorId(UUID idVehiculo) {
        Vehiculo vehiculo = buscarVehiculoOrThrow(idVehiculo);
        return mapearAResponse(vehiculo);
    }

    // ── ACTUALIZAR ─────────────────────────────────────────────────
    @Transactional
    public VehiculoResponseDTO actualizar(UUID idVehiculo, VehiculoRequestDTO dto) {

        Vehiculo vehiculo = buscarVehiculoOrThrow(idVehiculo);
        User usuarioAutenticado = obtenerUsuarioAutenticado();

        validarPropietario(vehiculo, usuarioAutenticado);

        String placaNormalizada = dto.placa().toUpperCase();

        // Si cambió la placa, valida que la nueva no choque con otro vehículo.
        if (!placaNormalizada.equals(vehiculo.getPlaca())
                && vehiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new RuntimeException("Ya existe un vehículo registrado con esa placa");
        }

        Marca marca = resolverMarca(dto.fkIdMarca(), dto.marcaSugerida(), usuarioAutenticado);

        vehiculo.setMarca(marca);
        vehiculo.setPlaca(placaNormalizada);
        vehiculo.setColor(dto.color());
        vehiculo.setTipoVehiculo(dto.tipoVehiculo());

        Vehiculo actualizado = vehiculoRepository.save(vehiculo);
        return mapearAResponse(actualizado);
    }

    // ── CAMBIAR ESTADO (borrado lógico) ────────────────────────────
    @Transactional
    public VehiculoResponseDTO cambiarEstado(UUID idVehiculo, boolean activo) {
        Vehiculo vehiculo = buscarVehiculoOrThrow(idVehiculo);
        User usuarioAutenticado = obtenerUsuarioAutenticado();

        validarPropietario(vehiculo, usuarioAutenticado);

        vehiculo.setEstado(activo);
        Vehiculo actualizado = vehiculoRepository.save(vehiculo);
        return mapearAResponse(actualizado);
    }

    // ── Utilidades privadas ─────────────────────────────────────────

    /**
     * Obtiene el usuario autenticado a partir del email guardado como
     * principal en el JWT (mismo patrón que el resto del sistema).
     */
    private User obtenerUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }

    private Vehiculo buscarVehiculoOrThrow(UUID idVehiculo) {
        return vehiculoRepository.findById(idVehiculo)
            .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
    }

    /**
     * Un usuario solo puede modificar sus propios vehículos.
     * NOTA: no distingue todavía rol ADMIN (pendiente anotado del tema
     * de roles/seguridad) — por ahora esto aplica igual para cualquiera.
     */
    private void validarPropietario(Vehiculo vehiculo, User usuarioAutenticado) {
        if (!vehiculo.getUsuario().getUserId().equals(usuarioAutenticado.getUserId())) {
            throw new RuntimeException("No tienes permiso para modificar este vehículo");
        }
    }

    /**
     * Resuelve la marca del vehículo a partir de EXACTAMENTE uno de los
     * dos campos del DTO:
     *  - fkIdMarca: marca ya existente y aprobada en el catálogo.
     *  - marcaSugerida: nombre nuevo, se crea con estado = false
     *    (pendiente de aprobación del admin) y queda ligada al usuario
     *    que la solicitó.
     */
    private Marca resolverMarca(UUID fkIdMarca, String marcaSugerida, User usuarioAutenticado) {

        boolean tieneIdMarca = fkIdMarca != null;
        boolean tieneMarcaSugerida = marcaSugerida != null && !marcaSugerida.isBlank();

        if (tieneIdMarca == tieneMarcaSugerida) {
            // true == true (ambos) o false == false (ninguno): ambos casos inválidos
            throw new RuntimeException(
                "Debes indicar una marca existente (fkIdMarca) o sugerir una nueva (marcaSugerida), pero no ambos ni ninguno"
            );
        }

        if (tieneIdMarca) {
            return marcaRepository.findById(fkIdMarca)
                .orElseThrow(() -> new RuntimeException("La marca seleccionada no existe"));
        }

        // Caso: marca sugerida por el cliente.
        String nombreNormalizado = marcaSugerida.trim().toUpperCase();

        // Si alguien más ya sugirió/tiene esa marca (sin importar su estado),
        // reutilizamos el registro en vez de crear un duplicado.
        return marcaRepository.findByNombreIgnoreCase(nombreNormalizado)
            .orElseGet(() -> {
                Marca nuevaMarca = new Marca();
                nuevaMarca.setNombre(nombreNormalizado);
                nuevaMarca.setEstado(false); // pendiente de aprobación del admin
                nuevaMarca.setUsuarioSolicitante(usuarioAutenticado);
                return marcaRepository.save(nuevaMarca);
            });
    }

    private VehiculoResponseDTO mapearAResponse(Vehiculo vehiculo) {
        return new VehiculoResponseDTO(
            vehiculo.getIdVehiculo(),
            vehiculo.getUsuario().getUserId(),
            vehiculo.getMarca().getIdMarca(),
            vehiculo.getMarca().getNombre(),
            vehiculo.getMarca().getEstado(),
            vehiculo.getPlaca(),
            vehiculo.getColor(),
            vehiculo.getTipoVehiculo(),
            vehiculo.getEstado(),
            vehiculo.getCreatedAt()
        );
    }
}
```

### src/main/java/com/lavarapido/backend_vehicular/vehiculos/controller/VehiculoController.java
```java
package com.lavarapido.backend_vehicular.vehiculos.controller;

import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoRequestDTO;
import com.lavarapido.backend_vehicular.vehiculos.dto.VehiculoResponseDTO;
import com.lavarapido.backend_vehicular.vehiculos.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;

    // ── Crear vehículo (app móvil — cliente autenticado) ────────────
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody VehiculoRequestDTO dto) {
        try {
            VehiculoResponseDTO creado = vehiculoService.crear(dto);
            return ResponseEntity.ok(creado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Mis vehículos (app móvil — cliente autenticado) ─────────────
    @GetMapping("/mis-vehiculos")
    public ResponseEntity<List<VehiculoResponseDTO>> listarMisVehiculos() {
        return ResponseEntity.ok(vehiculoService.listarMisVehiculos());
    }

    // ── Listar todos (web admin) ─────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<VehiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(vehiculoService.listarTodos());
    }

    // ── Obtener por id ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(vehiculoService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ── Actualizar ─────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody VehiculoRequestDTO dto) {
        try {
            VehiculoResponseDTO actualizado = vehiculoService.actualizar(id, dto);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Cambiar estado (activar / desactivar — borrado lógico) ──────
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo) {
        try {
            VehiculoResponseDTO actualizado = vehiculoService.cambiarEstado(id, activo);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
```

### src/main/java/com/lavarapido/backend_vehicular/vehiculos/enums/TipoVehiculo.java
```java
package com.lavarapido.backend_vehicular.vehiculos.enums;

public enum TipoVehiculo {
    CARRO,
    CAMIONETA,
    MOTO,
    MOTOCARRO,
    FURGONETA,
    PESADO
}
```
