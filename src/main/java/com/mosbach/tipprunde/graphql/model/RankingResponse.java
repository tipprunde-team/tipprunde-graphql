package com.mosbach.tipprunde.graphql.model;

import java.util.List;

// Antwort des Core auf GET /api/groups/{groupId}/ranking
public record RankingResponse(long groupId, Integer matchday, List<RankingEntry> ranking) {
}
