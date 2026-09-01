package com.lavarapido.backend_vehicular.reservas.exception;

public class VehiculoNoPerteneceUsuarioException extends RuntimeException {
    public VehiculoNoPerteneceUsuarioException(String message) {
        super(message);
    }
}
