package com.lavarapido.backend_vehicular.vehiculos.service;


import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.marcas.repository.MarcaRepository;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import com.lavarapido.backend_vehicular.users.repository.UserRoleRepository;
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
    private final UserRoleRepository userRoleRepository;

    // ── CREAR ──────────────────────────────────────────────────────
    @Transactional
    public VehiculoResponseDTO crear(VehiculoRequestDTO dto) {

        User usuarioAutenticado = obtenerUsuarioAutenticado();

        // Normaliza la placa a mayúsculas (el CHECK de la BD exige [A-Z]).
        String placaNormalizada = dto.placa().toUpperCase();

        if (vehiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new RuntimeException("Ya existe un vehículo registrado con esa placa");
        }

        Marca marca = resolverMarca(dto.fkIdMarca());

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
        User usuarioAutenticado = obtenerUsuarioAutenticado();

        validarPropietarioOAdmin(vehiculo, usuarioAutenticado);
        return mapearAResponse(vehiculo);
    }

    // ── ACTUALIZAR ─────────────────────────────────────────────────
    @Transactional
    public VehiculoResponseDTO actualizar(UUID idVehiculo, VehiculoRequestDTO dto) {

        Vehiculo vehiculo = buscarVehiculoOrThrow(idVehiculo);
        User usuarioAutenticado = obtenerUsuarioAutenticado();

        validarPropietarioOAdmin(vehiculo, usuarioAutenticado);

        String placaNormalizada = dto.placa().toUpperCase();

        // Si cambió la placa, valida que la nueva no choque con otro vehículo.
        if (!placaNormalizada.equals(vehiculo.getPlaca())
                && vehiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new RuntimeException("Ya existe un vehículo registrado con esa placa");
        }

        Marca marca = resolverMarca(dto.fkIdMarca());

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

        validarPropietarioOAdmin(vehiculo, usuarioAutenticado);

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
     * Un usuario puede modificar su propio vehículo. Un ADMIN puede
     * modificar el de cualquiera (necesario para el panel web).
     */
    private void validarPropietarioOAdmin(Vehiculo vehiculo, User usuarioAutenticado) {

        boolean esDueño = vehiculo.getUsuario().getUserId().equals(usuarioAutenticado.getUserId());
        if (esDueño) {
            return;
        }

        boolean esAdmin = userRoleRepository.findActiveRoleByUserId(usuarioAutenticado.getUserId())
            .map(ur -> "ADMIN".equals(ur.getRole().getRoleName()))
            .orElse(false);

        if (!esAdmin) {
            throw new RuntimeException("No tienes permiso para modificar este vehículo");
        }
    }

    /**
     * Resuelve la marca del vehículo a partir del catálogo aprobado.
     * El cliente ya no puede sugerir una marca nueva desde aquí: si no
     * la encuentra, debe pedirle al admin que la agregue desde
     * POST /api/marcas.
     */
    private Marca resolverMarca(UUID fkIdMarca) {
        return marcaRepository.findById(fkIdMarca)
            .orElseThrow(() -> new RuntimeException("La marca seleccionada no existe"));
    }

    private VehiculoResponseDTO mapearAResponse(Vehiculo vehiculo) {

        User usuario = vehiculo.getUsuario();

        boolean tieneApellido = usuario.getLastName() != null && !usuario.getLastName().isBlank();
        String nombreUsuario = tieneApellido
            ? usuario.getFirstName() + " " + usuario.getLastName()
            : usuario.getFirstName();

        return new VehiculoResponseDTO(
            vehiculo.getIdVehiculo(),
            usuario.getUserId(),
            nombreUsuario,
            usuario.getEmail(),
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