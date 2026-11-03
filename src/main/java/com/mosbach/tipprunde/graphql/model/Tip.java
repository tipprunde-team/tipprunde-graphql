package com.mosbach.tipprunde.graphql.model;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Gespeicherter Tipp, updatedAt ist die Serverzeit
public record Tip(long matchId, int home, int away, String updatedAt) {
}
