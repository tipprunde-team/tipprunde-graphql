package com.mosbach.tipprunde.graphql.model;

// Eintrag aus GET /api/groups: Gruppe aus Sicht des angemeldeten Nutzers
public record GroupSummary(long id, String name, String role, int memberCount, int myRank, int myPoints) {
}
