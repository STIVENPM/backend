package com.lavarapido.backend_vehicular.auditoria.repository;
import com.lavarapido.backend_vehicular.auditoria.entity.Auditoria;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID>, JpaSpecificationExecutor<Auditoria> { }
