package com.mosbach.tipprunde.graphql.model;

// Antwort auf den Login: das Token wird nie geloggt
public record TokenAnswer(String token, String expiresAt, User user) {
}
