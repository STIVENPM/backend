package com.lavarapido.backend_vehicular.pagos.exception;

/** Falta una clave Wompi o su prefijo no coincide con el ambiente; responde HTTP 500. */
public class WompiConfiguracionException extends RuntimeException {

    public WompiConfiguracionException(String message) {
        super(message);
    }
}
