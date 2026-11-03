package com.mosbach.tipprunde.graphql.model;

import java.util.List;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Spieltag einer Gruppe, serverTime entscheidet über die Sperre (nie die Uhr des Clients)
public record Matchday(int matchday, String serverTime, List<MatchEntry> matches) {
}
