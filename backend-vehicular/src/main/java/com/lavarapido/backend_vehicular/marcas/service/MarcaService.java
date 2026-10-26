package com.lavarapido.backend_vehicular.marcas.service;


import com.lavarapido.backend_vehicular.marcas.dto.MarcaRequestDTO;
import com.lavarapido.backend_vehicular.marcas.dto.MarcaResponseDTO;
import com.lavarapido.backend_vehicular.marcas.entity.Marca;
import com.lavarapido.backend_vehicular.marcas.repository.MarcaRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MarcaService {

    private final MarcaRepository marcaRepository;

    public MarcaService(MarcaRepository marcaRepository) { this.marcaRepository = marcaRepository; }

    // ── CREAR (admin — marca nace ya aprobada) ────────────────────
    // Normaliza el nombre y crea una marca activa.
    @Transactional
    public MarcaResponseDTO crear(MarcaRequestDTO dto) {

        String nombreNormalizado = dto.nombre().trim().toUpperCase();

        if (marcaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new IllegalStateException("Ya existe una marca con ese nombre");
        }

        Marca marca = new Marca();
        marca.setNombre(nombreNormalizado);
        marca.setEstado(true); // el admin la crea directamente aprobada
        marca.setUsuarioSolicitante(null);

        Marca guardada = marcaRepository.save(marca);
        return mapearAResponse(guardada);
    }

    // ── LISTAR ACTIVAS (catálogo — cliente + admin) ────────────────
    // Entrega solo marcas disponibles en el catálogo.
    public List<MarcaResponseDTO> listarActivas() {
        return marcaRepository.findByEstadoTrue()
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── LISTAR PENDIENTES (panel admin — solicitudes por revisar) ──
    // Entrega el catálogo completo para administración.
    public List<MarcaResponseDTO> listarTodas() {
        return marcaRepository.findAll()
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // Consulta marcas pendientes de aprobación.
    public List<MarcaResponseDTO> listarPendientes() {
        return marcaRepository.findByEstadoFalse()
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── BUSCAR (admin — barra de búsqueda del catálogo completo) ───
    // Busca marcas por nombre.
    public List<MarcaResponseDTO> buscar(String nombre) {
        return marcaRepository.findByNombreContainingIgnoreCase(nombre)
            .stream()
            .map(this::mapearAResponse)
            .toList();
    }

    // ── OBTENER POR ID ───────────────────────────────────────────────
    // Recupera una marca por su identificador.
    public MarcaResponseDTO obtenerPorId(UUID idMarca) {
        Marca marca = buscarMarcaOrThrow(idMarca);
        return mapearAResponse(marca);
    }

    // ── ACTUALIZAR (admin — ej. corregir nombre mal escrito antes de aprobar) ──
    // Evita nombres duplicados y guarda el cambio.
    @Transactional
    public MarcaResponseDTO actualizar(UUID idMarca, MarcaRequestDTO dto) {

        Marca marca = buscarMarcaOrThrow(idMarca);
        String nombreNormalizado = dto.nombre().trim().toUpperCase();

        // Si cambió el nombre, valida que no choque con otra marca existente.
        if (!nombreNormalizado.equals(marca.getNombre())
                && marcaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new IllegalStateException("Ya existe una marca con ese nombre");
        }

        marca.setNombre(nombreNormalizado);

        Marca actualizada = marcaRepository.save(marca);
        return mapearAResponse(actualizada);
    }

    // ── CAMBIAR ESTADO (aprobar una pendiente / activar-desactivar) ──
    // Aprueba o desactiva la marca sin eliminarla.
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
            .orElseThrow(() -> new RecursoNoEncontradoException("Marca no encontrada"));
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
