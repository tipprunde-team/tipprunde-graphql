package com.mosbach.tipprunde.graphql.model;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Zeile der Rangliste, gleiche Punkte haben gleichen Rang (1, 2, 2, 4)
public record RankingEntry(int rank, long userId, String username, int points, int exactTips) {
}
