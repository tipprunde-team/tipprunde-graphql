package com.mosbach.tipprunde.graphql.core;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Fehlerformat des Core: {"error":{"code":"...","message":"..."}}
public record CoreErrorResponse(Body error) {

    public record Body(String code, String message) {
    }
}
