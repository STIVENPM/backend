package com.lavarapido.backend_vehicular.reservas.exception;

public class ReservaCancelacionConflictException extends RuntimeException {
    private final String code;

    public ReservaCancelacionConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
