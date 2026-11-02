package com.mosbach.tipprunde.graphql.model;

import java.util.List;

// Spieltag einer Gruppe, serverTime entscheidet über die Sperre (nie die Uhr des Clients)
public record Matchday(int matchday, String serverTime, List<MatchEntry> matches) {
}
