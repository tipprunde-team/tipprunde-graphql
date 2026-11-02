package com.mosbach.tipprunde.graphql.model;

// Gespeicherter Tipp, updatedAt ist die Serverzeit
public record Tip(long matchId, int home, int away, String updatedAt) {
}
