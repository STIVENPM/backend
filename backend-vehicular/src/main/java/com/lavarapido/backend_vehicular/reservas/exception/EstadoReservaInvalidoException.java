package com.lavarapido.backend_vehicular.reservas.exception;

/** Senala una transicion de estado no permitida; la API responde HTTP 400. */
public class EstadoReservaInvalidoException extends RuntimeException {
    public EstadoReservaInvalidoException(String message) {
        super(message);
    }
}
