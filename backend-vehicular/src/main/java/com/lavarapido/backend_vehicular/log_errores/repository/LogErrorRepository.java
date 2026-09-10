package com.lavarapido.backend_vehicular.log_errores.repository;
import com.lavarapido.backend_vehicular.log_errores.entity.LogError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;
public interface LogErrorRepository extends JpaRepository<LogError, UUID>, JpaSpecificationExecutor<LogError> { }
