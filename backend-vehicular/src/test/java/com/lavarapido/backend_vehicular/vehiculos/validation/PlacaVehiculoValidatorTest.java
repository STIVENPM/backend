package com.lavarapido.backend_vehicular.vehiculos.validation;

import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import com.lavarapido.backend_vehicular.vehiculos.exception.PlacaVehiculoInvalidaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlacaVehiculoValidatorTest {

    private final PlacaVehiculoValidator validator = new PlacaVehiculoValidator();

    @Test
    void acceptsAndNormalizesSupportedVehicleFormats() {
        assertEquals("ABC123", validator.normalizarYValidar(" abc123 ", TipoVehiculo.CARRO));
        assertEquals("DEF456", validator.normalizarYValidar("def456", TipoVehiculo.CAMIONETA));
        assertEquals("GHI789", validator.normalizarYValidar("ghi789", TipoVehiculo.FURGONETA));
        assertEquals("JKL012", validator.normalizarYValidar("jkl012", TipoVehiculo.PESADO));
        assertEquals("MNO345", validator.normalizarYValidar("mno345", TipoVehiculo.MOTOCARRO));
        assertEquals("PQR67S", validator.normalizarYValidar("pqr67s", TipoVehiculo.MOTO));
    }

    @Test
    void rejectsHistoricalFormatsThatTheCurrentSchemaDoesNotSupport() {
        assertThrows(PlacaVehiculoInvalidaException.class,
                () -> validator.normalizarYValidar("AB1234", TipoVehiculo.CARRO));
        assertThrows(PlacaVehiculoInvalidaException.class,
                () -> validator.normalizarYValidar("AB1234", TipoVehiculo.MOTO));
        assertThrows(PlacaVehiculoInvalidaException.class,
                () -> validator.normalizarYValidar("AB1234", TipoVehiculo.MOTOCARRO));
    }

    @Test
    void rejectsFormatsThatBelongToAnotherVehicleTypeAndSpecialCharacters() {
        assertThrows(PlacaVehiculoInvalidaException.class,
                () -> validator.normalizarYValidar("ABC12D", TipoVehiculo.CARRO));
        assertThrows(PlacaVehiculoInvalidaException.class,
                () -> validator.normalizarYValidar("ABC123", TipoVehiculo.MOTO));
        assertThrows(PlacaVehiculoInvalidaException.class,
                () -> validator.normalizarYValidar("ABC-123", TipoVehiculo.CAMIONETA));
        assertThrows(PlacaVehiculoInvalidaException.class,
                () -> validator.normalizarYValidar("ABC 123", TipoVehiculo.PESADO));
    }
}
