package com.lavarapido.backend_vehicular.shared.exception;

import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import com.lavarapido.backend_vehicular.auth.exception.UserNotFoundException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiConfiguracionException;
import com.lavarapido.backend_vehicular.pagos.exception.PagoConflictException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiEventoInvalidoException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiProveedorException;
import com.lavarapido.backend_vehicular.reservas.exception.HorarioReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.EstadoReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.VehiculoNoPerteneceUsuarioException;
import com.lavarapido.backend_vehicular.users.exception.CurrentPasswordInvalidException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
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
        return response(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleEmailDelivery(EmailDeliveryException exception) {
        logger.warn("Error enviando correo de recuperacion ({})", exception.getClass().getSimpleName());
        return response(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible procesar la solicitud");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(RecursoNoEncontradoException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "No tienes permiso para esta operacion");
    }

    @ExceptionHandler(VehiculoNoPerteneceUsuarioException.class)
    public ResponseEntity<Map<String, String>> handleVehicleOwnership(VehiculoNoPerteneceUsuarioException exception) {
        return response(HttpStatus.FORBIDDEN, "No tienes permiso para esta operacion");
    }

    @ExceptionHandler({HorarioReservaInvalidoException.class, EstadoReservaInvalidoException.class})
    public ResponseEntity<Map<String, String>> handleInvalidReservation(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, "Horario o estado de reserva invalido");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException exception) {
        return response(HttpStatus.UNAUTHORIZED, "Credenciales o sesion invalidas");
    }

    @ExceptionHandler(CurrentPasswordInvalidException.class)
    public ResponseEntity<Map<String, String>> handleCurrentPasswordInvalid(CurrentPasswordInvalidException exception) {
        return response(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INVALID");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente");
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
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "Evento o transaccion de pago invalida");
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "Solicitud invalida");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        return response(HttpStatus.CONFLICT, "La operacion entra en conflicto con el estado actual");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        logger.error("Error inesperado al procesar solicitud", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
