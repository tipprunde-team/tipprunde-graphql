package com.mosbach.tipprunde.graphql.core;

// Fehlerformat des Core: {"error":{"code":"...","message":"..."}}
public record CoreErrorResponse(Body error) {

    public record Body(String code, String message) {
    }
}
