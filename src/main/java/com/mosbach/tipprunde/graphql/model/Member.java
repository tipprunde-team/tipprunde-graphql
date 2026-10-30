package com.mosbach.tipprunde.graphql.model;

// Mitglied einer Gruppe, joinedAt als ISO-Text in UTC
public record Member(long userId, String username, String role, String joinedAt) {
}
