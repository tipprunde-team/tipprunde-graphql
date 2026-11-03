package com.mosbach.tipprunde.graphql.model;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Vorschau vor dem Beitritt, ohne Anmeldung
public record InvitePreview(long groupId, String groupName, int memberCount) {
}
