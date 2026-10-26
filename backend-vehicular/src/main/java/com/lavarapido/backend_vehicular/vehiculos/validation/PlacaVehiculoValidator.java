package com.lavarapido.backend_vehicular.vehiculos.validation;

import com.lavarapido.backend_vehicular.vehiculos.enums.TipoVehiculo;
import com.lavarapido.backend_vehicular.vehiculos.exception.PlacaVehiculoInvalidaException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza y valida las placas colombianas soportadas por el catálogo de vehículos.
 * La validación se mantiene aquí, y no en el DTO, porque depende del tipo de vehículo.
 */
@Component
public class PlacaVehiculoValidator {

    private static final Pattern PLACA_AUTOMOTOR = Pattern.compile("^[A-Z]{3}\\d{3}$");
    private static final Pattern PLACA_MOTO = Pattern.compile("^[A-Z]{3}\\d{2}[A-Z]$");

    public String normalizarYValidar(String placa, TipoVehiculo tipoVehiculo) {
        if (placa == null || placa.isBlank()) {
            throw new PlacaVehiculoInvalidaException("La placa es obligatoria");
        }
        if (tipoVehiculo == null) {
            throw new PlacaVehiculoInvalidaException("El tipo de vehículo es obligatorio");
        }

        String placaNormalizada = placa.strip().toUpperCase(Locale.ROOT);
        boolean esValida = switch (tipoVehiculo) {
            case MOTO -> PLACA_MOTO.matcher(placaNormalizada).matches();
            case MOTOCARRO -> PLACA_AUTOMOTOR.matcher(placaNormalizada).matches();
            case CARRO, CAMIONETA, FURGONETA, PESADO -> PLACA_AUTOMOTOR.matcher(placaNormalizada).matches();
        };

        if (!esValida) {
            throw new PlacaVehiculoInvalidaException(mensajePara(tipoVehiculo));
        }

        return placaNormalizada;
    }

    private String mensajePara(TipoVehiculo tipoVehiculo) {
        if (tipoVehiculo == TipoVehiculo.MOTO) {
            return "Para MOTO, la placa debe tener el formato ABC12D";
        }
        return "Para " + tipoVehiculo + ", la placa debe tener el formato ABC123";
    }
}
