package com.example.galdcup.monitoring;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.galdcup.monitoring.MonitoringData.*;

/** 조회 대상과 PromQL은 서버에서 고정한다. 사용자 URL이나 임의 쿼리를 전달하지 않는다. */
@Component
public class PrometheusClient {
    private static final String HTTP = "{job=\"galdcup-spring\",uri!~\"/actuator.*|/api/admin/monitoring.*\"}";
    private static final String COUNT = "http_server_requests_seconds_count";
    private static final String REQUESTS = "sum(rate(" + COUNT + HTTP + "[5m]))";
    private static final String AVERAGE = "1000*sum(rate(http_server_requests_seconds_sum" + HTTP + "[5m]))/" + REQUESTS;
    private static final Map<String, String> QUERIES = queries();
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final URI base;

    @Autowired
    public PrometheusClient(ObjectMapper mapper) {
        this(mapper, URI.create("http://127.0.0.1:9090"));
    }

    PrometheusClient(ObjectMapper mapper, URI base) {
        this.mapper = mapper;
        this.base = base;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private static Map<String, String> queries() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("springUp", "max(up{job=\"galdcup-spring\"})");
        q.put("springAge", "time()-max(timestamp(up{job=\"galdcup-spring\"}))");
        q.put("exporterUp", "max(up{job=\"galdcup-redis\"})");
        q.put("redisAge", "time()-max(timestamp(up{job=\"galdcup-redis\"}))");
        q.put("redisUp", "max(redis_up{job=\"galdcup-redis\"})");
        q.put("rps", REQUESTS);
        q.put("averageMs", AVERAGE);
        q.put("maxMs", "1000*max(max_over_time(http_server_requests_seconds_max" + HTTP + "[5m]))");
        q.put("clientErrors", errorPercent("4.."));
        q.put("serverErrors", errorPercent("5.."));
        q.put("rateLimited", "(sum(rate(" + COUNT + HTTP.replace("}", ",status=\"429\"}") + "[5m])) or (0*" + REQUESTS + "))");
        q.put("memory", "max(redis_memory_used_bytes{job=\"galdcup-redis\"})");
        q.put("maxMemory", "max(redis_memory_max_bytes{job=\"galdcup-redis\"})");
        q.put("clients", "sum(redis_connected_clients{job=\"galdcup-redis\"})");
        String hits = "sum(rate(redis_keyspace_hits_total{job=\"galdcup-redis\"}[5m]))";
        String misses = "sum(rate(redis_keyspace_misses_total{job=\"galdcup-redis\"}[5m]))";
        q.put("hitPercent", "100*" + hits + "/(" + hits + "+" + misses + ")");
        return Map.copyOf(q);
    }

    private static String errorPercent(String status) {
        String filtered = HTTP.replace("}", ",status=~\"" + status + "\"}");
        // 요청이 있지만 해당 오류 시계열이 아직 없는 경우에만 분자를 0으로 처리한다.
        return "100*(sum(rate(" + COUNT + filtered + "[5m])) or (0*" + REQUESTS + "))/" + REQUESTS;
    }

    private static String labeled(String key, String expression) {
        return "label_replace((" + expression + "),\"metric\",\"" + key + "\",\"\",\"\")";
    }

    public Map<String, Double> current() throws IOException, InterruptedException {
        String query = QUERIES.entrySet().stream().map(e -> labeled(e.getKey(), e.getValue()))
                .collect(java.util.stream.Collectors.joining(" or "));
        JsonNode result = request("query", Map.of("query", query), "vector");
        Map<String, Double> values = new LinkedHashMap<>();
        for (JsonNode row : result) {
            String key = row.path("metric").path("metric").asString();
            if (QUERIES.containsKey(key)) values.put(key, finite(row.path("value").path(1)));
        }
        return values;
    }

    public History history(String range) throws IOException, InterruptedException {
        long seconds = rangeSeconds(range);
        long end = Instant.now().getEpochSecond();
        String query = labeled("rps", REQUESTS) + " or " + labeled("averageMs", AVERAGE);
        JsonNode result = request("query_range", Map.of("query", query, "start", String.valueOf(end - seconds),
                "end", String.valueOf(end), "step", String.valueOf(Math.max(15, seconds / 120))), "matrix");
        Map<String, List<Point>> series = new LinkedHashMap<>();
        for (JsonNode row : result) {
            String key = row.path("metric").path("metric").asString();
            if (!List.of("rps", "averageMs").contains(key)) continue;
            List<Point> points = new ArrayList<>();
            for (JsonNode value : row.path("values")) {
                if (points.size() >= 150) break;
                points.add(new Point(value.path(0).asLong() * 1000, finite(value.path(1))));
            }
            series.put(key, List.copyOf(points));
        }
        return new History(true, Map.copyOf(series));
    }

    public static long rangeSeconds(String range) {
        return switch (range) {
            case "30m" -> 1800;
            case "1h" -> 3600;
            case "6h" -> 21600;
            default -> throw new IllegalArgumentException("조회 기간은 30m, 1h, 6h 중 선택해 주세요.");
        };
    }

    private JsonNode request(String path, Map<String, String> parameters, String type)
            throws IOException, InterruptedException {
        String query = parameters.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(base.resolve("/api/v1/" + path + "?" + query))
                .timeout(Duration.ofSeconds(3)).header("Accept", "application/json").GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 || response.body().length() > 1_000_000)
            throw new IOException("Prometheus response unavailable");
        JsonNode json = mapper.readTree(response.body());
        if (!"success".equals(json.path("status").asString())
                || !type.equals(json.path("data").path("resultType").asString())
                || !json.path("data").path("result").isArray())
            throw new IOException("Invalid Prometheus response");
        return json.path("data").path("result");
    }

    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }

    private static Double finite(JsonNode value) {
        try {
            double number = Double.parseDouble(value.asString());
            return Double.isFinite(number) ? number : null;
        } catch (NumberFormatException | IllegalStateException e) { return null; }
    }
}
