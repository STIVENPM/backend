package com.lavarapido.backend_vehicular.auth.exception;

/** Distingue una cuenta ausente; GlobalExceptionHandler la traduce a HTTP 404. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
