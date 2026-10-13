package com.lavarapido.backend_vehicular.shared.exception;

/** Recurso buscado por un servicio que no existe; la API conserva el mensaje en HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
