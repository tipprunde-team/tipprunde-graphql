package com.mosbach.tipprunde.graphql.core;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.mosbach.tipprunde.graphql.model.User;

import reactor.core.publisher.Mono;

// Aufrufe des Core-Service. Der Header Authorization wird unverändert weitergereicht, geprüft wird er nur im Core.
@Component
public class CoreClient {

    private final WebClient webClient;

    public CoreClient(WebClient coreWebClient) {
        this.webClient = coreWebClient;
    }

    public Mono<User> me(String authorization) {
        return get("/api/users/me", authorization, User.class);
    }

    private <T> Mono<T> get(String uri, String authorization, Class<T> type) {
        return handle(webClient.get().uri(uri).headers(headers -> authorize(headers, authorization)).retrieve(), type);
    }

    // Ohne Header kein Authorization-Header, der Core antwortet dann selbst mit 401
    private static void authorize(HttpHeaders headers, String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    // Fehlerantworten des Core ({"error":{"code","message"}}) werden zu CoreApiException
    private <T> Mono<T> handle(WebClient.ResponseSpec spec, Class<T> type) {
        return spec.onStatus(HttpStatusCode::isError, this::toException).bodyToMono(type);
    }

    private Mono<Throwable> toException(ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(CoreErrorResponse.class)
                .onErrorResume(e -> Mono.empty())
                .map(body -> (Throwable) new CoreApiException(status, body.error().code(), body.error().message()))
                .switchIfEmpty(Mono.fromSupplier(() -> new CoreApiException(status, "CORE_ERROR",
                        "Der Core-Service hat mit einem Fehler geantwortet.")));
    }
}
