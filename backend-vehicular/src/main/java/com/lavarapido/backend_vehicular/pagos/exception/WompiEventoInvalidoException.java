package com.lavarapido.backend_vehicular.pagos.exception;

/** Evento o transaccion de pago no supera las validaciones; responde HTTP 422. */
public class WompiEventoInvalidoException extends RuntimeException {
    public WompiEventoInvalidoException(String message) {
        super(message);
    }
}
