package com.lavarapido.backend_vehicular.pagos.config;

import com.lavarapido.backend_vehicular.pagos.exception.WompiConfiguracionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WompiConfigurationService {
    private final WompiProperties properties;

    public void validarWidget() {
        Ambiente ambiente = ambiente();
        validarPrefijo(properties.getPublicKey(), ambiente.publicKeyPrefix, "llave publica");
        validarPrefijo(properties.getIntegritySecret(), ambiente.integrityPrefix, "secreto de integridad");
    }

    public void validarEventos() {
        Ambiente ambiente = ambiente();
        validarPrefijo(properties.getEventsSecret(), ambiente.eventsPrefix, "secreto de eventos");
    }

    public void validarConsulta() {
        Ambiente ambiente = ambiente();
        validarPrefijo(properties.getPrivateKey(), ambiente.privateKeyPrefix, "llave privada");
    }

    public String ambienteEventoEsperado() {
        return ambiente().eventValue;
    }

    public String apiBaseUrl() {
        return ambiente().apiBaseUrl;
    }

    private Ambiente ambiente() {
        String configured = properties.getEnvironment();
        if (configured == null) {
            throw new WompiConfiguracionException("No esta configurado el ambiente de Wompi");
        }
        return switch (configured.trim().toLowerCase(Locale.ROOT)) {
            case "sandbox", "test" -> Ambiente.SANDBOX;
            case "production", "prod" -> Ambiente.PRODUCTION;
            default -> throw new WompiConfiguracionException("El ambiente de Wompi no es valido");
        };
    }

    private void validarPrefijo(String value, String prefix, String field) {
        if (value == null || value.isBlank()) {
            throw new WompiConfiguracionException("No esta configurado " + field + " de Wompi");
        }
        if (!value.startsWith(prefix)) {
            throw new WompiConfiguracionException(field + " de Wompi no corresponde al ambiente configurado");
        }
    }

    private enum Ambiente {
        SANDBOX("test", "https://sandbox.wompi.co/v1", "pub_test_", "prv_test_", "test_integrity_", "test_events_"),
        PRODUCTION("prod", "https://production.wompi.co/v1", "pub_prod_", "prv_prod_", "prod_integrity_", "prod_events_");

        private final String eventValue;
        private final String apiBaseUrl;
        private final String publicKeyPrefix;
        private final String privateKeyPrefix;
        private final String integrityPrefix;
        private final String eventsPrefix;

        Ambiente(String eventValue, String apiBaseUrl, String publicKeyPrefix, String privateKeyPrefix,
                 String integrityPrefix, String eventsPrefix) {
            this.eventValue = eventValue;
            this.apiBaseUrl = apiBaseUrl;
            this.publicKeyPrefix = publicKeyPrefix;
            this.privateKeyPrefix = privateKeyPrefix;
            this.integrityPrefix = integrityPrefix;
            this.eventsPrefix = eventsPrefix;
        }
    }
}
