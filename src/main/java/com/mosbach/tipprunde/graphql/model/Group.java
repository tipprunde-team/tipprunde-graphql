package com.mosbach.tipprunde.graphql.model;

import java.util.List;

// Gruppe wie der Core sie liefert: inviteCode nur für Admins, members nur bei den Gruppendetails, role nur beim Anlegen
public record Group(long id, String name, String role, String inviteCode, List<Member> members) {
}
