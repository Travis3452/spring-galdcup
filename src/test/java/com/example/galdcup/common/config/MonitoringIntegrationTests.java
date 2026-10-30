package com.example.galdcup.common.config;

import com.example.galdcup.common.security.JwtAuthenticationFilter;
import com.example.galdcup.common.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 실제 두 HTTP 서버를 띄우되 업무 도메인·DB·Redis·외부 OAuth는 로드하지 않는다. */
@Timeout(60)
class MonitoringIntegrationTests {

    private static ConfigurableApplicationContext application;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static int apiPort;
    private static int managementPort;
    private static JwtTokenProvider tokens;

    @BeforeAll
    static void startIsolatedServers() {
        application = new SpringApplicationBuilder(TestApplication.class).run(
                "--server.port=0",
                "--server.address=127.0.0.1",
                "--management.server.port=0",
                "--cors.allowed-origins=http://localhost",
                "--jwt.secret=monitoring-test-only-key-0123456789abcdef0123456789abcdef",
                "--logging.level.root=ERROR",
                "--logging.level.com.example.galdcup=ERROR",
                "--spring.main.banner-mode=off");
        apiPort = application.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        managementPort = application.getEnvironment().getRequiredProperty("local.management.port", Integer.class);
        tokens = application.getBean(JwtTokenProvider.class);
    }

    @AfterAll
    static void stopServers() {
        if (application != null) application.close();
    }

    @Test
    void monitoringDashboardRequiresAdminForEveryEndpoint() throws Exception {
        String admin = tokens.createAccessToken(1L, List.of("ADMIN"));
        for (String path : List.of("/api/admin/monitoring", "/api/admin/monitoring/health",
                "/api/admin/monitoring/api", "/api/admin/monitoring/redis", "/api/admin/monitoring/alerts")) {
            assertEquals(401, send(apiPort, "GET", path, null).statusCode(), path);
            for (String role : List.of("USER", "MANAGER")) {
                assertEquals(403, send(apiPort, "GET", path,
                        tokens.createAccessToken(2L, List.of(role))).statusCode(), path + role);
            }
            var response = send(apiPort, "GET", path, admin);
            assertEquals(200, response.statusCode(), path);
            assertEquals("no-store", response.headers().firstValue("cache-control").orElse(""));
        }
        assertEquals(401, send(apiPort, "GET", "/api/admin/monitoring", tokens.createRefreshToken(1L)).statusCode());
        assertEquals(400, send(apiPort, "GET", "/api/admin/monitoring?range=365d", admin).statusCode());
    }

    @Test
    void productionConfigurationUsesSeparateLoopbackManagementServer() throws Exception {
        var properties = new YamlPropertySourceLoader()
                .load("production", new ClassPathResource("application.yml")).get(0);
        assertEquals(8080, properties.getProperty("server.port"));
        assertEquals(9091, properties.getProperty("management.server.port"));
        assertEquals("127.0.0.1", properties.getProperty("management.server.address"));
        assertEquals("health,prometheus", properties.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("never", properties.getProperty("management.endpoint.health.show-details"));
        assertNotEquals(apiPort, managementPort);
        assertEquals("127.0.0.1", application.getEnvironment().getProperty("management.server.address"));
    }

    @Test
    void managementHealthAndScrapeAreAnonymousButOtherEndpointsAreUnavailable() throws Exception {
        var health = send(managementPort, "GET", "/actuator/health", null);
        assertEquals(200, health.statusCode());
        var body = application.getBean(tools.jackson.databind.ObjectMapper.class).readTree(health.body());
        assertEquals("UP", body.path("status").asText());
        assertFalse(body.has("components"));
        assertFalse(body.has("details"));
        assertEquals(200, send(managementPort, "GET", "/actuator/prometheus", null).statusCode());
        for (String path : List.of("/actuator", "/actuator/env", "/actuator/beans", "/actuator/heapdump")) {
            assertTrue(send(managementPort, "GET", path, null).statusCode() >= 400, path);
        }
        assertTrue(send(managementPort, "POST", "/actuator/prometheus", null).statusCode() >= 400);
        assertTrue(send(managementPort, "GET", "/api/monitoring-test/1", null).statusCode() >= 400);
    }

    @Test
    void apiPortNeverServesMetricsEvenWithAdminTokenOrForwardedHeaders() throws Exception {
        String admin = tokens.createAccessToken(1L, List.of("ADMIN"));
        for (String path : List.of("/actuator/prometheus", "/actuator/health", "/actuator/env")) {
            assertTrue(send(apiPort, "GET", path, null).statusCode() >= 400, path);
            assertEquals(403, send(apiPort, "GET", path, admin).statusCode(), path);
        }
        var spoofed = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + apiPort + "/actuator/prometheus"))
                .header("Forwarded", "host=127.0.0.1:" + managementPort + ";proto=http")
                .header("X-Forwarded-Port", String.valueOf(managementPort))
                .timeout(Duration.ofSeconds(5)).GET().build();
        assertTrue(CLIENT.send(spoofed, HttpResponse.BodyHandlers.ofString()).statusCode() >= 400);
    }

    @Test
    void existingPublicReadsAndJwtProtectedWritesKeepTheirAuthenticationRules() throws Exception {
        assertEquals(200, send(apiPort, "GET", "/api/monitoring-test/1", null).statusCode());
        assertEquals(401, send(apiPort, "POST", "/api/monitoring-test/1", null).statusCode());
        assertEquals(200, send(apiPort, "POST", "/api/monitoring-test/1",
                tokens.createAccessToken(7L, List.of("USER"))).statusCode());
        assertEquals(401, send(apiPort, "POST", "/api/monitoring-test/1", tokens.createRefreshToken(7L)).statusCode());
        assertEquals(401, send(apiPort, "POST", "/api/monitoring-test/1", "malformed.jwt").statusCode());
    }

    @Test
    void realRequestsProducePrometheusTimersWithRouteTemplates() throws Exception {
        assertEquals(200, send(apiPort, "GET", "/api/monitoring-test/12345", null).statusCode());
        assertEquals(200, send(apiPort, "GET", "/api/monitoring-test/67890", null).statusCode());
        String scrape = send(managementPort, "GET", "/actuator/prometheus", null).body();
        for (String name : List.of("http_server_requests_seconds_count",
                "http_server_requests_seconds_sum", "http_server_requests_seconds_max")) {
            assertTrue(scrape.lines().anyMatch(line -> line.startsWith(name + "{")
                    && line.contains("uri=\"/api/monitoring-test/{id}\"")
                    && line.contains("method=\"GET\"") && line.contains("status=\"200\"")), name);
        }
        assertFalse(scrape.contains("uri=\"/api/monitoring-test/12345\""));
        assertFalse(scrape.contains("uri=\"/api/monitoring-test/67890\""));
    }

    private static HttpResponse<String> send(int port, String method, String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return CLIENT.send(builder.method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
            "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration",
            "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
            "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
    })
    @Import({SecurityConfig.class, ActuatorSecurityConfig.class,
            JwtTokenProvider.class, JwtAuthenticationFilter.class, ProbeController.class,
            com.example.galdcup.monitoring.MonitoringController.class,
            com.example.galdcup.monitoring.MonitoringService.class,
            com.example.galdcup.monitoring.DependencyHealth.class,
            com.example.galdcup.common.exception.GlobalExceptionHandler.class})
    static class TestApplication {
        @org.springframework.context.annotation.Bean
        com.example.galdcup.monitoring.PrometheusClient prometheusClient() throws Exception {
            var client = org.mockito.Mockito.mock(com.example.galdcup.monitoring.PrometheusClient.class);
            org.mockito.Mockito.when(client.current()).thenReturn(java.util.Map.of());
            org.mockito.Mockito.when(client.history(org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(new com.example.galdcup.monitoring.MonitoringData.History(true, java.util.Map.of()));
            return client;
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/monitoring-test/{id}")
        String read(@PathVariable String id) {
            return "ok";
        }

        @PostMapping("/api/monitoring-test/{id}")
        String write(@PathVariable String id) {
            return "ok";
        }
    }
}
