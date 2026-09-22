package com.lavarapido.backend_vehicular.pagos.entity;

import com.lavarapido.backend_vehicular.pagos.enums.EstadoIntentoPago;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "pago_intentos",
        indexes = {
                @Index(name = "idx_pago_intentos_pago", columnList = "fk_id_pago"),
                @Index(name = "idx_pago_intentos_transaccion", columnList = "wompi_environment,wompi_transaction_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pago_intentos_referencia",
                columnNames = "referencia"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoIntento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_intento", nullable = false, updatable = false)
    private UUID idIntento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_pago", nullable = false)
    private Pago pago;

    @Column(name = "referencia", nullable = false, length = 100)
    private String referencia;

    @Column(name = "wompi_transaction_id", length = 100)
    private String wompiTransactionId;

    @Column(name = "wompi_payment_method_type", length = 50)
    private String wompiPaymentMethodType;

    @Column(name = "wompi_status", length = 20)
    private String wompiStatus;

    @Column(name = "wompi_environment", nullable = false, length = 10)
    private String wompiEnvironment;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 24)
    @Builder.Default
    private EstadoIntentoPago estado = EstadoIntentoPago.pendiente;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = ahora;
        }
        updatedAt = ahora;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
