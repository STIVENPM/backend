package com.lavarapido.backend_vehicular.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SwaggerUiDependencyTest {

    @Test
    void fixedSwaggerUiWebJarIsAvailableOnTheRuntimeClasspath() {
        assertNotNull(
                getClass().getClassLoader().getResource(
                        "META-INF/resources/webjars/swagger-ui/5.32.14/index.html"));
    }
}
