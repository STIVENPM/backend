package com.lavarapido.backend_vehicular.pagos.service;

import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.exception.WompiProveedorException;
import com.sun.net.httpserver.HttpServer;
import java.time.Duration;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WompiTransactionClientImplTest {
    @Test
    void usesBoundedConnectionAndReadTimeouts() {
        assertEquals(Duration.ofSeconds(5), WompiTransactionClientImpl.CONNECT_TIMEOUT);
        assertEquals(Duration.ofSeconds(10), WompiTransactionClientImpl.READ_TIMEOUT);
    }

    @Test
    void mapsProviderErrorsToDomainException() {
        WompiProperties properties = properties();
        WompiConfigurationService configuration = mock(WompiConfigurationService.class);
        when(configuration.apiBaseUrl()).thenReturn("http://wompi.test");
        doNothing().when(configuration).validarConsulta();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://wompi.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://wompi.test/transactions/tx-1"))
                .andExpect(header(AUTHORIZATION, "Bearer prv_test_example"))
                .andRespond(withServerError());

        WompiTransactionClientImpl client = new WompiTransactionClientImpl(
                properties, configuration, builder.build());

        assertThrows(WompiProveedorException.class, () -> client.consultar("tx-1"));
        server.verify();
    }

    @Test
    void returnsTransactionDataFromProvider() {
        WompiProperties properties = properties();
        WompiConfigurationService configuration = mock(WompiConfigurationService.class);
        when(configuration.apiBaseUrl()).thenReturn("http://wompi.test");
        doNothing().when(configuration).validarConsulta();
        RestClient.Builder builder = RestClient.builder().baseUrl("http://wompi.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://wompi.test/transactions/tx-2"))
                .andRespond(withSuccess("{\"data\":{\"id\":\"tx-2\"}}", APPLICATION_JSON));

        WompiTransactionClientImpl client = new WompiTransactionClientImpl(
                properties, configuration, builder.build());

        assertEquals("tx-2", client.consultar("tx-2").path("id").asString());
        server.verify();
    }

    @Test
    void mapsAControlledSlowProviderToDomainException() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/transactions/slow", exchange -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        var executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.start();

        try {
            WompiProperties properties = properties();
            WompiConfigurationService configuration = mock(WompiConfigurationService.class);
            doNothing().when(configuration).validarConsulta();
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(500));
            requestFactory.setReadTimeout(Duration.ofMillis(50));
            RestClient client = RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .requestFactory(requestFactory)
                    .build();

            WompiTransactionClientImpl transactionClient = new WompiTransactionClientImpl(
                    properties, configuration, client);

            assertThrows(WompiProveedorException.class, () -> transactionClient.consultar("slow"));
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private WompiProperties properties() {
        WompiProperties properties = new WompiProperties();
        properties.setPrivateKey("prv_test_example");
        return properties;
    }
}
