package com.lavarapido.backend_vehicular.shared.exception;

import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import com.lavarapido.backend_vehicular.auth.exception.UserNotFoundException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiConfiguracionException;
import com.lavarapido.backend_vehicular.pagos.exception.PagoConflictException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiEventoInvalidoException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiProveedorException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleEmailDelivery(EmailDeliveryException exception) {
        logger.error("Error enviando correo de recuperacion", exception);
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(RecursoNoEncontradoException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDatabase(DataAccessException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible acceder a la base de datos");
    }

    @ExceptionHandler(WompiConfiguracionException.class)
    public ResponseEntity<Map<String, String>> handleWompiConfiguracion(WompiConfiguracionException exception) {
        logger.error("Error de configuración de Wompi", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error de configuración del servicio de pagos");
    }

    @ExceptionHandler(PagoConflictException.class)
    public ResponseEntity<Map<String, String>> handlePagoConflict(PagoConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(WompiEventoInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleWompiEventoInvalido(WompiEventoInvalidoException exception) {
        logger.warn("Evento o transaccion Wompi rechazado: {}", exception.getMessage());
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(WompiProveedorException.class)
    public ResponseEntity<Map<String, String>> handleWompiProveedor(WompiProveedorException exception) {
        logger.error("Error consultando Wompi", exception);
        return response(HttpStatus.BAD_GATEWAY, "No fue posible consultar el estado en Wompi");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("La solicitud no es valida");
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
