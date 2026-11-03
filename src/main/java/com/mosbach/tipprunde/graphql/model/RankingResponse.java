package com.mosbach.tipprunde.graphql.model;

import java.util.List;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Antwort des Core auf GET /api/groups/{groupId}/ranking
public record RankingResponse(long groupId, Integer matchday, List<RankingEntry> ranking) {
}
