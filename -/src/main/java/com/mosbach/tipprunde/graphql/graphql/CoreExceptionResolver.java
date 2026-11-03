package com.mosbach.tipprunde.graphql.graphql;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.mosbach.tipprunde.graphql.core.CoreApiException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

// Gibt Fehler des Core mit demselben code als GraphQL-Fehler zurück (extensions.code), z. B. TIP_LOCKED
@Component
public class CoreExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(CoreExceptionResolver.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof CoreApiException core) {
            // Abgelehnte Zugriffe sichtbar machen, nie Token, Passwort oder Argumente loggen
            if (core.getStatus() == 401 || core.getStatus() == 403) {
                log.warn("Access denied: status={} code={} field={}", core.getStatus(), core.getCode(),
                        env.getField().getName());
            }
            return error(env, errorType(core.getStatus()), core.getMessage(), core.getCode());
        }
        if (ex instanceof WebClientRequestException || ex instanceof TimeoutException) {
            log.warn("Core not reachable: field={} cause={}", env.getField().getName(), ex.getClass().getSimpleName());
            return error(env, ErrorType.INTERNAL_ERROR, "Der Core-Service ist nicht erreichbar.", "CORE_UNAVAILABLE");
        }
        return null;
    }

    private static ErrorType errorType(int status) {
        return switch (status) {
            case 400 -> ErrorType.BAD_REQUEST;
            case 401 -> ErrorType.UNAUTHORIZED;
            case 403 -> ErrorType.FORBIDDEN;
            case 404 -> ErrorType.NOT_FOUND;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }

    private static GraphQLError error(DataFetchingEnvironment env, ErrorType type, String message, String code) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
