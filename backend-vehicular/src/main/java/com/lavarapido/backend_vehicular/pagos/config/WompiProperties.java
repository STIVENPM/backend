package com.lavarapido.backend_vehicular.pagos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "wompi")
public class WompiProperties {
    private String publicKey;
    /** Reserved for a future direct-API integration. It is deliberately unused by the Widget flow. */
    private String privateKey;
    private String environment;
    private String integritySecret;
    private String eventsSecret;
}
