package com.lavarapido.backend_vehicular.pagos.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Executes a payment state transition in a short database transaction. */
@Service
public class PagoTransactionExecutor {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T execute(Supplier<T> operation) {
        return operation.get();
    }
}
