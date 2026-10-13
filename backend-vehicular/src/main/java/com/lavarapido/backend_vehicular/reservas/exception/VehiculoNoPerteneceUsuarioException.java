package com.lavarapido.backend_vehicular.reservas.exception;

/** Impide reservar con un vehiculo ajeno; GlobalExceptionHandler devuelve HTTP 403. */
public class VehiculoNoPerteneceUsuarioException extends RuntimeException {
    public VehiculoNoPerteneceUsuarioException(String message) {
        super(message);
    }
}
