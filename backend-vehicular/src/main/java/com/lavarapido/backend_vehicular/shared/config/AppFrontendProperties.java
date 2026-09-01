package com.lavarapido.backend_vehicular.shared.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.frontend")
public class AppFrontendProperties {

    @NotBlank(message = "app.frontend.url no puede estar vacío")
    private String url;
}
