package com.lavarapido.backend_vehicular.pagos.service;

import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.exception.WompiProveedorException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class WompiTransactionClientImpl implements WompiTransactionClient {
    // Implementacion HTTP del contrato usado por verificar y reconciliar.
    private final WompiProperties properties;
    private final WompiConfigurationService configurationService;

    // Consulta a Wompi con la clave privada y devuelve la transacción.
    @Override
    public JsonNode consultar(String transactionId) {
        // Antes de llamar al proveedor comprueba que exista una llave privada
        // con el prefijo del ambiente Wompi seleccionado.
        configurationService.validarConsulta();
        try {
            // La llamada GET se hace desde el backend con Bearer privado;
            // el navegador no recibe esa credencial.
            JsonNode response = RestClient.create(configurationService.apiBaseUrl())
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
