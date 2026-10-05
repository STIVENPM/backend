package com.lavarapido.backend_vehicular.asignaciones.entity;

import com.lavarapido.backend_vehicular.asignaciones.enums.EstadoAsignacion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter
public class EstadoAsignacionConverter implements AttributeConverter<EstadoAsignacion, String> {

    @Override
    public String convertToDatabaseColumn(EstadoAsignacion estado) {
        return estado == null ? null : estado.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public EstadoAsignacion convertToEntityAttribute(String estado) {
        return estado == null ? null : EstadoAsignacion.valueOf(estado.toLowerCase(Locale.ROOT));
    }
}
