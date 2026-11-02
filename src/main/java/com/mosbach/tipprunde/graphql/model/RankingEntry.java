package com.mosbach.tipprunde.graphql.model;

// Zeile der Rangliste, gleiche Punkte haben gleichen Rang (1, 2, 2, 4)
public record RankingEntry(int rank, long userId, String username, int points, int exactTips) {
}
