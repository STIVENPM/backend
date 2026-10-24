package com.lavarapido.backend_vehicular.pagos.service;

import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.exception.WompiProveedorException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Duration;

@Component
public class WompiTransactionClientImpl implements WompiTransactionClient {
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    // Implementacion HTTP del contrato usado por verificar y reconciliar.
    private final WompiProperties properties;
    private final WompiConfigurationService configurationService;
    private final RestClient restClient;

    @Autowired
    public WompiTransactionClientImpl(WompiProperties properties,
            WompiConfigurationService configurationService) {
        this(properties, configurationService, createClient(configurationService.apiBaseUrl()));
    }

    WompiTransactionClientImpl(WompiProperties properties,
            WompiConfigurationService configurationService, RestClient restClient) {
        this.properties = properties;
        this.configurationService = configurationService;
        this.restClient = restClient;
    }

    private static RestClient createClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    // Consulta a Wompi con la clave privada y devuelve la transacción.
    @Override
    public JsonNode consultar(String transactionId) {
        // Antes de llamar al proveedor comprueba que exista una llave privada
        // con el prefijo del ambiente Wompi seleccionado.
        configurationService.validarConsulta();
        try {
            // La llamada GET se hace desde el backend con Bearer privado;
            // el navegador no recibe esa credencial.
            JsonNode response = restClient
                    .get()
                    .uri("/transactions/{id}", transactionId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getPrivateKey())
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("data").isObject()) {
                // Sin el objeto data no hay estado remoto confiable.
                throw new WompiProveedorException("Wompi devolvio una respuesta sin transaccion", null);
            }
            return response.path("data");
        } catch (WompiProveedorException exception) {
            // Conserva el error de formato que se detecto en esta misma clase.
            throw exception;
        } catch (RestClientException exception) {
            // Traduce errores HTTP o de red a una excepcion del dominio pagos.
            throw new WompiProveedorException("No fue posible consultar la transaccion en Wompi", exception);
        }
    }
}
