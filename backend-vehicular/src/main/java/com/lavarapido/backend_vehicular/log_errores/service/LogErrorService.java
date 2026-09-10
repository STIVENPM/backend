package com.lavarapido.backend_vehicular.log_errores.service;
import com.lavarapido.backend_vehicular.log_errores.dto.LogErrorResponseDTO;
import com.lavarapido.backend_vehicular.log_errores.entity.LogError;
import com.lavarapido.backend_vehicular.log_errores.enums.TipoError;
import com.lavarapido.backend_vehicular.log_errores.repository.LogErrorRepository;
import com.lavarapido.backend_vehicular.shared.exception.RecursoNoEncontradoException;
import com.lavarapido.backend_vehicular.users.entity.User;
import com.lavarapido.backend_vehicular.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class LogErrorService {
    private final LogErrorRepository repository; private final UserRepository userRepository;
    @Transactional public void registrar(UUID usuarioId, TipoError tipo, String descripcion, String ipOrigen) {
        User usuario = usuarioId == null ? null : userRepository.findById(usuarioId).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        repository.save(LogError.builder().usuario(usuario).tipoError(tipo).descripcion(descripcion).ipOrigen(ipOrigen).build());
    }
    @Transactional(readOnly = true) public Page<LogErrorResponseDTO> listar(Boolean resuelto, Pageable pageable) {
        Specification<LogError> spec = resuelto == null
                ? (root, query, criteriaBuilder) -> criteriaBuilder.conjunction()
                : (root, q, cb) -> cb.equal(root.get("resuelto"), resuelto);
        return repository.findAll(spec, pageable).map(this::map);
    }
    @Transactional public LogErrorResponseDTO resolver(UUID id) { LogError error = repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Error no encontrado")); error.setResuelto(true); return map(repository.save(error)); }
    private LogErrorResponseDTO map(LogError e) { return new LogErrorResponseDTO(e.getIdError(), e.getUsuario() == null ? null : e.getUsuario().getUserId(), e.getTipoError(), e.getDescripcion(), e.getIpOrigen(), e.getResuelto(), e.getCreatedAt(), e.getUpdatedAt()); }
}
