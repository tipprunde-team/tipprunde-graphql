package com.mosbach.tipprunde.graphql;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Kleiner Platzhalter für den Core-Service (JDK-HttpServer), damit die Tests ohne Core und ohne Datenbank laufen.
// Antworten werden je "METHODE /pfad" festgelegt, alle Aufrufe werden mit Header und Body aufgezeichnet.
public class MockCore {

    public record Reply(int status, String body) {
    }

    public record Call(String method, String path, String query, String authorization, String body) {
    }

    private final HttpServer server;
    private final Map<String, Reply> replies = new ConcurrentHashMap<>();
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final AtomicInteger count = new AtomicInteger();

    public MockCore() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }

    public void reply(String methodAndPath, int status, String body) {
        replies.put(methodAndPath, new Reply(status, body));
    }

    public void reset() {
        replies.clear();
        calls.clear();
        count.set(0);
    }

    public List<Call> calls() {
        return calls;
    }

    public long callsTo(String methodAndPath) {
        return calls.stream().filter(c -> (c.method() + " " + c.path()).equals(methodAndPath)).count();
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        calls.add(new Call(method, path, exchange.getRequestURI().getQuery(),
                exchange.getRequestHeaders().getFirst("Authorization"), body));
        count.incrementAndGet();
        Reply reply = replies.getOrDefault(method + " " + path,
                new Reply(404, "{\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"Der Pfad existiert nicht.\"}}"));
        byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(reply.status(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }
}
