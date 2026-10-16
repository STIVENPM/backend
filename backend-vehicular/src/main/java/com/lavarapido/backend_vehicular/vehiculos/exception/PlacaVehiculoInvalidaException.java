package com.lavarapido.backend_vehicular.vehiculos.exception;

/** Se lanza cuando la placa no corresponde al tipo de vehículo informado. */
public class PlacaVehiculoInvalidaException extends RuntimeException {

    public PlacaVehiculoInvalidaException(String message) {
        super(message);
    }
}
