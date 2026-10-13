package com.lavarapido.backend_vehicular.reservas.exception;

/** Senala horario ausente, fuera del rango o solapado; la API responde HTTP 400. */
public class HorarioReservaInvalidoException extends RuntimeException {
    public HorarioReservaInvalidoException(String message) {
        super(message);
    }
}
