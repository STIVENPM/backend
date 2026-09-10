package com.lavarapido.backend_vehicular.auditoria.service;
import com.lavarapido.backend_vehicular.auditoria.dto.AuditoriaResponseDTO;
import com.lavarapido.backend_vehicular.auditoria.entity.Auditoria;
import com.lavarapido.backend_vehicular.auditoria.enums.*;
import com.lavarapido.backend_vehicular.auditoria.repository.AuditoriaRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class AuditoriaService {
    private final AuditoriaRepository repository; private final UserRepository userRepository;
    @Transactional public void registrar(UUID usuarioId, AccionAuditoria accion, String descripcion, String ipOrigen, ModuloAuditoria modulo) {
        User usuario = usuarioId == null ? null : userRepository.findById(usuarioId).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        repository.save(Auditoria.builder().usuario(usuario).accion(accion).descripcion(descripcion).ipOrigen(ipOrigen).modulo(modulo).build());
    }
    @Transactional(readOnly = true) public Page<AuditoriaResponseDTO> listar(UUID usuarioId, AccionAuditoria accion, ModuloAuditoria modulo, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        Specification<Auditoria> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (usuarioId != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("usuario").get("userId"), usuarioId));
        if (accion != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("accion"), accion));
        if (modulo != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("modulo"), modulo));
        if (fechaDesde != null) spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fechaDesde));
        if (fechaHasta != null) spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), fechaHasta));
        return repository.findAll(spec, pageable).map(this::map);
    }
    private AuditoriaResponseDTO map(Auditoria a) { return new AuditoriaResponseDTO(a.getIdAuditoria(), a.getUsuario() == null ? null : a.getUsuario().getUserId(), a.getAccion(), a.getDescripcion(), a.getIpOrigen(), a.getModulo(), a.getCreatedAt()); }
}
