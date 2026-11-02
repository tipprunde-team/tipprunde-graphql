package com.mosbach.tipprunde.graphql.model;

// Ein Spiel des Spieltags mit gesperrt-Status und eigenem Tipp (myTip und myPoints sind null ohne Tipp bzw. ohne Wertung)
public record MatchEntry(
        long id,
        String kickoff,
        String status,
        boolean locked,
        Team homeTeam,
        Team awayTeam,
        Score result,
        Score myTip,
        Integer myPoints) {

    public record Team(String name, String shortName, String crest) {
    }

    public record Score(int home, int away) {
    }
}
