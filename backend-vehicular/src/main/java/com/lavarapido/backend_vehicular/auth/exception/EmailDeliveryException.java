package com.lavarapido.backend_vehicular.auth.exception;

/** Falla el envio de recuperacion; conserva la causa y responde HTTP 503. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public String failureCategory() {
        Throwable root = rootCause();
        String type = root == null ? "unknown" : root.getClass().getSimpleName().toLowerCase();
        if (type.contains("authentication")) return "authentication";
        if (type.contains("ssl") || type.contains("tls")) return "tls";
        if (type.contains("connect")) return "connection";
        if (type.contains("timeout")) return "timeout";
        return "delivery";
    }

    public String rootCauseType() {
        Throwable root = rootCause();
        return root == null ? "unknown" : root.getClass().getName();
    }

    private Throwable rootCause() {
        Throwable root = getCause();
        while (root != null && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
}
