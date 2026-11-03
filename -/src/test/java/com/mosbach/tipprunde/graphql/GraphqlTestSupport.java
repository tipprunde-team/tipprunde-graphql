package com.mosbach.tipprunde.graphql;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.env.Environment;

import com.jayway.jsonpath.JsonPath;

// Sendet GraphQL-Anfragen per HTTP an den laufenden Testserver und liest die Antwort mit JsonPath
public class GraphqlTestSupport {

    private final HttpClient client = HttpClient.newHttpClient();
    private final Environment environment;

    public GraphqlTestSupport(Environment environment) {
        this.environment = environment;
    }

    public record Result(int status, String json) {
        public <T> T get(String path) {
            return JsonPath.read(json, path);
        }

        public boolean has(String path) {
            try {
                Object value = JsonPath.read(json, path);
                return value != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

    // query ist der GraphQL-Text, authorization der komplette Header (oder null)
    public Result post(String query, String authorization) {
        try {
            String payload = "{\"query\":" + JsonPath.parse(Map.of("q", query)).jsonString().replaceFirst("^\\{\"q\":", "")
                    .replaceFirst("}$", "") + "}";
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + environment.getProperty("local.server.port") + "/graphql"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            if (authorization != null) {
                builder.header("Authorization", authorization);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Result(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
