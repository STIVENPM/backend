package com.lavarapido.backend_vehicular.users.exception;

public class CurrentPasswordInvalidException extends RuntimeException {
    public CurrentPasswordInvalidException() {
        super("CURRENT_PASSWORD_INVALID");
    }
}
