package com.mosbach.tipprunde.graphql.model;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Antwort auf den Login: das Token wird nie geloggt
public record TokenAnswer(String token, String expiresAt, User user) {
}
