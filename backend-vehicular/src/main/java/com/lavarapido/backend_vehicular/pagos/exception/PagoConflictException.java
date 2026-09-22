package com.lavarapido.backend_vehicular.pagos.exception;

public class PagoConflictException extends RuntimeException {
    public PagoConflictException(String message) {
        super(message);
    }
}
