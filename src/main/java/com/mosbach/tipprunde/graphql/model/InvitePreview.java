package com.mosbach.tipprunde.graphql.model;

// Vorschau vor dem Beitritt, ohne Anmeldung
public record InvitePreview(long groupId, String groupName, int memberCount) {
}
