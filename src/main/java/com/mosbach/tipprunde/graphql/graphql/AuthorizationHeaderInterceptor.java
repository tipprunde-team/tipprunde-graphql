package com.mosbach.tipprunde.graphql.graphql;

import java.util.Collections;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

// Legt den Header Authorization in den GraphQL-Kontext, damit ihn die Resolver an den Core weiterreichen können
@Component
public class AuthorizationHeaderInterceptor implements WebGraphQlInterceptor {

    public static final String CONTEXT_KEY = "authorization";

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            request.configureExecutionInput((input, builder) ->
                    builder.graphQLContext(Collections.singletonMap(CONTEXT_KEY, authorization)).build());
        }
        return chain.next(request);
    }
}
