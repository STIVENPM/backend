package com.lavarapido.backend_vehicular.pagos.service;

import com.lavarapido.backend_vehicular.pagos.config.WompiConfigurationService;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;
import com.lavarapido.backend_vehicular.pagos.controller.PagoController;
import com.lavarapido.backend_vehicular.pagos.repository.PagoIntentoRepository;
import com.lavarapido.backend_vehicular.pagos.repository.PagoRepository;
import com.lavarapido.backend_vehicular.reservas.repository.ReservaRepository;
import com.lavarapido.backend_vehicular.security.AccountAccessService;
import com.lavarapido.backend_vehicular.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class WompiSpringContextTest {
    @Test
    void createsWompiPaymentGraphWithSpringConstructorSelection() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            assertInstanceOf(WompiTransactionClientImpl.class,
                    context.getBean(WompiTransactionClient.class));
            assertNotNull(context.getBean(PagoService.class));
            assertNotNull(context.getBean(PagoController.class));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(WompiTransactionClientImpl.class)
    static class TestConfiguration {
        @Bean
        WompiProperties wompiProperties() {
            WompiProperties properties = new WompiProperties();
            properties.setEnvironment("sandbox");
            properties.setPrivateKey("prv_test_context");
            properties.setPublicKey("pub_test_context");
            properties.setIntegritySecret("test_integrity_context");
            properties.setEventsSecret("test_events_context");
            return properties;
        }

        @Bean
        WompiConfigurationService wompiConfigurationService(WompiProperties properties) {
            return new WompiConfigurationService(properties);
        }

        @Bean
        WompiSignatureService wompiSignatureService(WompiProperties properties,
                WompiConfigurationService configurationService) {
            return new WompiSignatureService(properties, configurationService);
        }

        @Bean
        PagoService pagoService(PagoRepository pagoRepository, PagoIntentoRepository intentoRepository,
                ReservaRepository reservaRepository, AccountAccessService accountAccessService,
                WompiProperties properties, WompiConfigurationService configurationService,
                WompiSignatureService signatureService, WompiTransactionClient transactionClient,
                NotificacionService notificacionService, PagoTransactionExecutor transactionExecutor) {
            return new PagoService(pagoRepository, intentoRepository, reservaRepository,
                    accountAccessService, properties, configurationService, signatureService,
                    transactionClient, notificacionService, transactionExecutor);
        }

        @Bean
        PagoController pagoController(PagoService pagoService, WompiSignatureService signatureService) {
            return new PagoController(pagoService, signatureService);
        }

        @Bean
        PagoRepository pagoRepository() { return mock(PagoRepository.class); }

        @Bean
        PagoIntentoRepository intentoRepository() { return mock(PagoIntentoRepository.class); }

        @Bean
        ReservaRepository reservaRepository() { return mock(ReservaRepository.class); }

        @Bean
        AccountAccessService accountAccessService() { return mock(AccountAccessService.class); }

        @Bean
        NotificacionService notificacionService() { return mock(NotificacionService.class); }

        @Bean
        PagoTransactionExecutor transactionExecutor() { return mock(PagoTransactionExecutor.class); }
    }
}
