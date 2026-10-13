package com.lavarapido.backend_vehicular.pagos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "wompi")
public class WompiProperties {
    // Spring agrupa aqui las propiedades wompi.* del perfil activo o del entorno.
    private String publicKey;
    /** Used only for server-side transaction status queries, never for creating charges. */
    private String privateKey;
    private String environment;
    // Integridad firma el widget; eventos verifica los avisos entrantes de Wompi.
    private String integritySecret;
    private String eventsSecret;
}
