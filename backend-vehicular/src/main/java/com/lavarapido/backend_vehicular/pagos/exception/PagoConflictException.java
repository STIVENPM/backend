package com.lavarapido.backend_vehicular.pagos.exception;

/** Estado o datos de pago incompatibles con la operacion; responde HTTP 409. */
public class PagoConflictException extends RuntimeException {
    public PagoConflictException(String message) {
        super(message);
    }
}
