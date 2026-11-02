package com.mosbach.tipprunde.graphql.core;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import com.mosbach.tipprunde.graphql.model.Group;
import com.mosbach.tipprunde.graphql.model.GroupSummary;
import com.mosbach.tipprunde.graphql.model.InvitePreview;
import com.mosbach.tipprunde.graphql.model.TokenAnswer;
import com.mosbach.tipprunde.graphql.model.User;

import reactor.core.publisher.Flux;
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

    public Mono<List<GroupSummary>> myGroups(String authorization) {
        return handle(webClient.get().uri("/api/groups").headers(h -> authorize(h, authorization)).retrieve(),
                GroupSummary.class).collectList();
    }

    public Mono<Group> group(String authorization, long groupId) {
        return get("/api/groups/" + groupId, authorization, Group.class);
    }

    // Öffentlich im Core, deshalb ohne Authorization-Header
    public Mono<InvitePreview> invitePreview(String code) {
        return handle(webClient.get().uri("/api/invites/{code}", code).retrieve(), InvitePreview.class).next();
    }

    // Registrierung und Login brauchen kein Token
    public Mono<User> register(String email, String username, String password) {
        return send(webClient.post().uri("/api/users"), null,
                Map.of("email", email, "username", username, "password", password), User.class);
    }

    public Mono<TokenAnswer> login(String email, String password) {
        return send(webClient.post().uri("/api/auth"), null, Map.of("email", email, "password", password),
                TokenAnswer.class);
    }

    public Mono<Group> createGroup(String authorization, String name) {
        return send(webClient.post().uri("/api/groups"), authorization, Map.of("name", name), Group.class);
    }

    public Mono<GroupSummary> joinGroup(String authorization, String inviteCode) {
        return send(webClient.post().uri("/api/groups/join"), authorization, Map.of("inviteCode", inviteCode),
                GroupSummary.class);
    }

    private <T> Mono<T> send(WebClient.RequestBodySpec spec, String authorization, Object body, Class<T> type) {
        return handle(spec.headers(h -> authorize(h, authorization)).bodyValue(body).retrieve(), type).next();
    }

    private <T> Mono<T> get(String uri, String authorization, Class<T> type) {
        return handle(webClient.get().uri(uri).headers(headers -> authorize(headers, authorization)).retrieve(), type)
                .next();
    }

    // Ohne Header kein Authorization-Header, der Core antwortet dann selbst mit 401
    private static void authorize(HttpHeaders headers, String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    // Fehlerantworten des Core ({"error":{"code","message"}}) werden zu CoreApiException
    private <T> Flux<T> handle(WebClient.ResponseSpec spec, Class<T> type) {
        return spec.onStatus(HttpStatusCode::isError, this::toException).bodyToFlux(type);
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
