package com.example.galdcup.monitoring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PrometheusClientTests {
    private HttpServer server;
    private PrometheusClient client;
    private String body;
    private int status;
    private final AtomicReference<String> request = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        status = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/", exchange -> {
            request.set(URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        client = new PrometheusClient(new ObjectMapper(), URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void parsesZeroAndMissingValuesWithoutInventingMeasurements() throws Exception {
        body = """
                {"status":"success","data":{"resultType":"vector","result":[
                  {"metric":{"metric":"rps"},"value":[1,"0"]},
                  {"metric":{"metric":"averageMs"},"value":[1,"NaN"]},
                  {"metric":{"metric":"maxMs"},"value":[1,"+Inf"]},
                  {"metric":{"metric":"memory"},"value":[1,"1658664"]},
                  {"metric":{"metric":"arbitrary"},"value":[1,"999"]}
                ]}}
                """;
        var values = client.current();
        assertEquals(0.0, values.get("rps"));
        assertNull(values.get("averageMs"));
        assertNull(values.get("maxMs"));
        assertEquals(1658664.0, values.get("memory"));
        assertFalse(values.containsKey("clients"));
        assertFalse(values.containsKey("arbitrary"));
        assertTrue(request.get().contains("/api/v1/query?"));
        assertTrue(request.get().contains("/api/admin/monitoring.*"));
        assertTrue(request.get().contains("status=\"429\""));
        assertTrue(request.get().contains("timestamp(up"));
    }

    @Test
    void parsesBoundedHistoryAndPreservesGaps() throws Exception {
        body = """
                {"status":"success","data":{"resultType":"matrix","result":[
                  {"metric":{"metric":"rps"},"values":[[1700000000,"1.25"],[1700000030,"NaN"]]}
                ]}}
                """;
        var history = client.history("6h");
        assertTrue(history.available());
        var points = history.series().get("rps");
        assertEquals(1700000000000L, points.get(0).timestamp());
        assertEquals(1.25, points.get(0).value());
        assertNull(points.get(1).value());
        assertTrue(request.get().contains("step=180"));
        assertThrows(IllegalArgumentException.class, () -> client.history("../../secrets"));
    }

    @Test
    void rejectsUnavailableMalformedAndWrongTypeResponses() {
        body = "{\"status\":\"error\",\"data\":{\"resultType\":\"vector\",\"result\":[]}}";
        assertThrows(IOException.class, client::current);
        body = "{\"status\":\"success\",\"data\":{\"resultType\":\"matrix\",\"result\":[]}}";
        assertThrows(IOException.class, client::current);
        status = 503;
        assertThrows(IOException.class, client::current);
        status = 302;
        assertThrows(IOException.class, client::current);
    }
}
