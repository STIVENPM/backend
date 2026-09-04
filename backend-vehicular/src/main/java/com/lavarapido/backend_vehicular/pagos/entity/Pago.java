package com.lavarapido.backend_vehicular.pagos.entity;

import com.lavarapido.backend_vehicular.pagos.enums.EstadoPago;
import com.lavarapido.backend_vehicular.reservas.entity.Reserva;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_pago", nullable = false, updatable = false)
    private UUID idPago;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_reserva", nullable = false, unique = true)
    private Reserva reserva;

    @Column(name = "metodo_pago", nullable = false, length = 10)
    @Builder.Default
    private String metodoPago = "online";

    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;

    @Column(name = "estado_wompi", length = 15)
    private String estadoWompi;

    @Column(name = "monto", nullable = false, precision = 12, scale = 0)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoPago estado = EstadoPago.pendiente;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
}
