package com.lavarapido.backend_vehicular.pagos.exception;

/** Falla la consulta al proveedor Wompi; conserva la causa y responde HTTP 502. */
public class WompiProveedorException extends RuntimeException {
    public WompiProveedorException(String message, Throwable cause) {
        super(message, cause);
    }
}
