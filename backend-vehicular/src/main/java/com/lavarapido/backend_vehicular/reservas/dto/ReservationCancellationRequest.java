package com.lavarapido.backend_vehicular.reservas.dto;

import jakarta.validation.constraints.Size;

/** Optional evidence supplied when the customer explicitly accepts a cancellation policy. */
public record ReservationCancellationRequest(
        Boolean policyAccepted,
        @Size(max = 50, message = "La versión de la política no puede superar 50 caracteres")
        String policyVersion
) {
    public boolean hasPolicyData() {
        return policyAccepted != null || (policyVersion != null && !policyVersion.isBlank());
    }

    public boolean isExplicitAcceptance() {
        return Boolean.TRUE.equals(policyAccepted)
                && policyVersion != null
                && !policyVersion.isBlank();
    }

    public String normalizedPolicyVersion() {
        return policyVersion == null ? null : policyVersion.trim();
    }
}
