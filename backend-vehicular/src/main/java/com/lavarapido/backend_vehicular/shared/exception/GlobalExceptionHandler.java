package com.lavarapido.backend_vehicular.shared.exception;

import com.lavarapido.backend_vehicular.auth.exception.EmailDeliveryException;
import com.lavarapido.backend_vehicular.auth.exception.UserNotFoundException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiConfiguracionException;
import com.lavarapido.backend_vehicular.pagos.exception.PagoConflictException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiEventoInvalidoException;
import com.lavarapido.backend_vehicular.pagos.exception.WompiProveedorException;
import com.lavarapido.backend_vehicular.reservas.exception.HorarioReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.EstadoReservaInvalidoException;
import com.lavarapido.backend_vehicular.reservas.exception.ReservaCancelacionConflictException;
import com.lavarapido.backend_vehicular.reservas.exception.VehiculoNoPerteneceUsuarioException;
import com.lavarapido.backend_vehicular.users.exception.CurrentPasswordInvalidException;
import com.lavarapido.backend_vehicular.vehiculos.exception.PlacaVehiculoInvalidaException;
import com.lavarapido.backend_vehicular.servicios.exception.DuplicateServiceNameException;
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

// Centraliza las excepciones que llegan desde controladores y servicios.
// Traduce cada tipo a un HTTP concreto con cuerpo {"error": ...}; no corrige el fallo.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception) {
        // Oculta el detalle de la cuenta ausente y responde 404.
        return response(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleEmailDelivery(EmailDeliveryException exception) {
        // Registra el tipo de fallo sin exponer la causa al cliente.
        logger.warn("Password reset email delivery failed: category={}, causeType={}",
                exception.failureCategory(), exception.rootCauseType());
        return response(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible procesar la solicitud");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(RecursoNoEncontradoException exception) {
        // Los servicios usan esta excepcion al buscar entidades inexistentes.
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        // La identidad puede estar autenticada, pero no autorizada para esa accion.
        return response(HttpStatus.FORBIDDEN, "No tienes permiso para esta operacion");
    }

    @ExceptionHandler(VehiculoNoPerteneceUsuarioException.class)
    public ResponseEntity<Map<String, String>> handleVehicleOwnership(VehiculoNoPerteneceUsuarioException exception) {
        // ReservaService bloquea el uso de un vehiculo de otro usuario.
        return response(HttpStatus.FORBIDDEN, "No tienes permiso para esta operacion");
    }

    @ExceptionHandler(HorarioReservaInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSchedule(HorarioReservaInvalidoException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EstadoReservaInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleInvalidReservation(EstadoReservaInvalidoException exception) {
        return response(HttpStatus.BAD_REQUEST, "Estado de reserva invalido");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException exception) {
        // Login o sesion invalidos: el cliente recibe 401 sin detalle del motivo.
        return response(HttpStatus.UNAUTHORIZED, "Credenciales o sesion invalidas");
    }

    @ExceptionHandler(CurrentPasswordInvalidException.class)
    public ResponseEntity<Map<String, String>> handleCurrentPasswordInvalid(CurrentPasswordInvalidException exception) {
        return response(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INVALID");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException exception) {
        // Una restriccion de BD, como una clave unica, se presenta como 409.
        return response(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente");
    }

    @ExceptionHandler(DuplicateServiceNameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateServiceName(
            DuplicateServiceNameException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDatabase(DataAccessException exception) {
        // Otros fallos de acceso a datos se separan del conflicto de integridad.
        return response(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible acceder a la base de datos");
    }

    @ExceptionHandler(WompiConfiguracionException.class)
    public ResponseEntity<Map<String, String>> handleWompiConfiguracion(WompiConfiguracionException exception) {
        // Error interno de claves o ambiente: el detalle queda en el log.
        // El cliente solo recibe un mensaje generico y HTTP 500.
        logger.error("Error de configuración de Wompi", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error de configuración del servicio de pagos");
    }

    @ExceptionHandler(PagoConflictException.class)
    public ResponseEntity<Map<String, String>> handlePagoConflict(PagoConflictException exception) {
        // Ejemplo: la reserva ya tiene pago aprobado o el intento cambio de estado.
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ReservaCancelacionConflictException.class)
    public ResponseEntity<Map<String, String>> handleReservationCancellationConflict(
            ReservaCancelacionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", exception.getCode(), "error", exception.getMessage()));
    }

    @ExceptionHandler(WompiEventoInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleWompiEventoInvalido(WompiEventoInvalidoException exception) {
        // Rechaza eventos y transacciones inconsistentes sin devolver su detalle.
        logger.warn("Evento o transaccion Wompi rechazado: {}", exception.getMessage());
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "Evento o transaccion de pago invalida");
    }

    @ExceptionHandler(WompiProveedorException.class)
    public ResponseEntity<Map<String, String>> handleWompiProveedor(WompiProveedorException exception) {
        // 502 diferencia un problema del proveedor externo de la BD local.
        logger.error("Error consultando Wompi", exception);
        return response(HttpStatus.BAD_GATEWAY, "No fue posible consultar el estado en Wompi");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        // Toma el primer error de validacion del DTO y lo muestra como HTTP 400.
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("La solicitud no es valida");
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(PlacaVehiculoInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleInvalidVehiclePlate(PlacaVehiculoInvalidaException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "Solicitud invalida");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        // Un estado que impide completar la operacion se comunica como conflicto.
        return response(HttpStatus.CONFLICT, "La operacion entra en conflicto con el estado actual");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        // Ultimo recurso: registra la traza y evita exponer detalles internos en HTTP.
        logger.error("Error inesperado al procesar solicitud", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        // Todos los handlers anteriores comparten el mismo contrato JSON de error.
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
