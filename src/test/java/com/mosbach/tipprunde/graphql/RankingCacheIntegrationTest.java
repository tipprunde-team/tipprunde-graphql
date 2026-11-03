package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Cache im Zusammenspiel mit Token und Mitgliedschaft: gecachte Daten gehen nie an Nichtmitglieder
class RankingCacheIntegrationTest extends AbstractGraphqlTest {

    private static final String GROUP_JSON = "{\"id\":3,\"name\":\"WG\",\"members\":[]}";
    private static final String RANKING_JSON = "{\"groupId\":3,\"matchday\":null,\"ranking\":["
            + "{\"rank\":1,\"userId\":7,\"username\":\"niklas\",\"points\":31,\"exactTips\":3}]}";
    private static final String QUERY = "{ ranking(groupId: 3) { rank username points } }";

    @Test
    void secondRankingQueryComesFromTheCacheButMembershipIsCheckedEveryTime() {
        core.reply("GET /api/groups/3", 200, GROUP_JSON);
        core.reply("GET /api/groups/3/ranking", 200, RANKING_JSON);

        var first = graphql.post(QUERY, "Bearer a");
        var second = graphql.post(QUERY, "Bearer a");

        assertEquals("niklas", first.get("$.data.ranking[0].username"));
        assertEquals("niklas", second.get("$.data.ranking[0].username"));
        assertEquals(1, core.callsTo("GET /api/groups/3/ranking"));
        assertEquals(2, core.callsTo("GET /api/groups/3"));
    }

    @Test
    void aNonMemberNeverGetsACachedRanking() {
        core.reply("GET /api/groups/3", 200, GROUP_JSON);
        core.reply("GET /api/groups/3/ranking", 200, RANKING_JSON);
        graphql.post(QUERY, "Bearer mitglied");

        core.reply("GET /api/groups/3", 403,
                "{\"error\":{\"code\":\"NOT_A_MEMBER\",\"message\":\"Du bist kein Mitglied dieser Tippgruppe.\"}}");
        var foreign = graphql.post(QUERY, "Bearer fremd");

        assertEquals("NOT_A_MEMBER", foreign.get("$.errors[0].extensions.code"));
        assertFalse(foreign.has("$.data.ranking[0]"));
        assertEquals(1, core.callsTo("GET /api/groups/3/ranking"));
    }

    @Test
    void aDifferentMatchdayIsLoadedSeparately() {
        core.reply("GET /api/groups/3", 200, GROUP_JSON);
        core.reply("GET /api/groups/3/ranking", 200, RANKING_JSON);

        graphql.post(QUERY, "Bearer a");
        graphql.post("{ ranking(groupId: 3, matchday: 6) { rank } }", "Bearer a");

        assertEquals(2, core.callsTo("GET /api/groups/3/ranking"));
    }

    @Test
    void anErrorFromTheCoreIsNotCached() {
        core.reply("GET /api/groups/3", 200, GROUP_JSON);
        core.reply("GET /api/groups/3/ranking", 500, "{\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"Fehler\"}}");
        graphql.post(QUERY, "Bearer a");

        core.reply("GET /api/groups/3/ranking", 200, RANKING_JSON);
        var second = graphql.post(QUERY, "Bearer a");

        assertEquals("niklas", second.get("$.data.ranking[0].username"));
        assertEquals(2, core.callsTo("GET /api/groups/3/ranking"));
    }
}
