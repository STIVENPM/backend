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