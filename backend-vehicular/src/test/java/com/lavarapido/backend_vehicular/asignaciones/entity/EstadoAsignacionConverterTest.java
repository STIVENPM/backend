package com.lavarapido.backend_vehicular.asignaciones.entity;

import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EstadoAsignacionConverterTest {

    private final EstadoAsignacionConverter converter = new EstadoAsignacionConverter();

    @Test
    void persistsDatabaseCheckConstraintValuesAndReadsThemBack() {
        for (EstadoAsignacion estado : EstadoAsignacion.values()) {
            String valorPersistido = converter.convertToDatabaseColumn(estado);

            assertEquals(estado.name().toUpperCase(Locale.ROOT), valorPersistido);
            assertEquals(estado, converter.convertToEntityAttribute(valorPersistido));
        }
    }

    @Test
    void preservesNullValues() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
