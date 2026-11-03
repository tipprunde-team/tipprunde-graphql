package com.mosbach.tipprunde.graphql.model;

import com.fasterxml.jackson.annotation.JsonProperty;

// Nutzer wie der Core ihn liefert; email und isSystemAdmin fehlen in der Login-Antwort
public record User(
        long id,
        String email,
        String username,
        @JsonProperty("isSystemAdmin") Boolean isSystemAdmin) {
}
