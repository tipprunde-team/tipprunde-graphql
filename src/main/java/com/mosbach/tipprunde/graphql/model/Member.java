package com.mosbach.tipprunde.graphql.model;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Mitglied einer Gruppe, joinedAt als ISO-Text in UTC
public record Member(long userId, String username, String role, String joinedAt) {
}
