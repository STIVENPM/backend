package com.lavarapido.backend_vehicular.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimitTomcatIntegrationTest {
    private static final String LOGIN_PATH = "/api/users/login";
    private static final String TEST_CLIENT = "198.51.100.10";

    private Tomcat tomcat;
    private HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @AfterEach
    void stopServer() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    @Test
    void trustedForwardedClientAddressIsUsedForTheRateLimitKey() throws Exception {
        startServer("127\\.0\\.0\\.1");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals(401, send(TEST_CLIENT).statusCode());
        }

        assertEquals(429, send(TEST_CLIENT).statusCode());
    }

    @Test
    void untrustedForwardedHeaderCannotChangeTheRateLimitKey() throws Exception {
        startServer("10\\.0\\.0\\.\\d+");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals(401, send("198.51.100." + (attempt + 10)).statusCode());
        }

        assertEquals(429, send("203.0.113.10").statusCode());
    }

    @Test
    void unauthorizedResponsesDoNotResetTheCounter() throws Exception {
        startServer("127\\.0\\.0\\.1");

        for (int attempt = 0; attempt < 5; attempt++) {
            HttpResponse<String> response = send(TEST_CLIENT);
            assertEquals(401, response.statusCode());
        }

        HttpResponse<String> blocked = send(TEST_CLIENT);
        assertEquals(429, blocked.statusCode());
        assertEquals("900", blocked.headers().firstValue("Retry-After").orElseThrow());
    }

    @Test
    void productionProfileUsesNativeForwardedHeadersStrategy() throws Exception {
        Properties properties = new Properties();
        try (var input = getClass().getResourceAsStream("/application-prod.properties")) {
            assertTrue(input != null);
            properties.load(input);
        }

        assertEquals("NATIVE", properties.getProperty("server.forward-headers-strategy"));
    }

    private void startServer(String internalProxies) throws Exception {
        Path baseDirectory = Files.createTempDirectory("rate-limit-tomcat-");
        tomcat = new Tomcat();
        tomcat.setBaseDir(baseDirectory.toString());
        Connector connector = tomcat.getConnector();
        connector.setPort(0);
        connector.setProperty("address", "127.0.0.1");

        Context context = tomcat.addContext("", baseDirectory.toString());

        RemoteIpValve remoteIpValve = new RemoteIpValve();
        remoteIpValve.setInternalProxies(internalProxies);
        context.getPipeline().addValve(remoteIpValve);

        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("authRateLimitFilter");
        filterDef.setFilter(new AuthRateLimitFilter());
        context.addFilterDef(filterDef);

        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName("authRateLimitFilter");
        filterMap.addURLPattern(LOGIN_PATH);
        context.addFilterMap(filterMap);

        Wrapper loginServlet = Tomcat.addServlet(context, "loginServlet", new FailedLoginServlet());
        loginServlet.setLoadOnStartup(1);
        context.addServletMappingDecoded(LOGIN_PATH, "loginServlet");

        tomcat.start();
    }

    private HttpResponse<String> send(String forwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + LOGIN_PATH))
                .timeout(Duration.ofSeconds(2))
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static class FailedLoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
        }
    }
}
