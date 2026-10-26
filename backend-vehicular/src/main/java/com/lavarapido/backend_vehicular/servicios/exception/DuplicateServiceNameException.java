package com.lavarapido.backend_vehicular.servicios.exception;

/** The service name already exists in the catalog. */
public class DuplicateServiceNameException extends RuntimeException {
    public DuplicateServiceNameException() {
        super("Ya existe un servicio con ese nombre");
    }
}
