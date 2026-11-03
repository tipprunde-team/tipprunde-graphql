package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
class MatchQueriesTest extends AbstractGraphqlTest {

    private static final String BEARER = "Bearer abc.def.ghi";

    // Antwort des Core auf die Mitgliedschaftsprobe GET /api/groups/3, die vor jedem Cache-Zugriff läuft
    private static final String GROUP_JSON = "{\"id\":3,\"name\":\"WG Lohrtalweg\",\"members\":[]}";

    private static final String MATCHDAY_JSON = "{\"matchday\":6,\"serverTime\":\"2026-10-17T12:00:00Z\",\"matches\":["
            + "{\"id\":101,\"kickoff\":\"2026-10-17T13:30:00Z\",\"status\":\"UPCOMING\",\"locked\":false,"
            + "\"homeTeam\":{\"name\":\"SC Freiburg\",\"shortName\":\"Freiburg\",\"crest\":\"https://crests.example/17.png\"},"
            + "\"awayTeam\":{\"name\":\"VfB Stuttgart\",\"shortName\":\"Stuttgart\",\"crest\":null},"
            + "\"result\":null,\"myTip\":{\"home\":2,\"away\":1},\"myPoints\":null},"
            + "{\"id\":98,\"kickoff\":\"2026-10-16T18:30:00Z\",\"status\":\"FINISHED\",\"locked\":true,"
            + "\"homeTeam\":{\"name\":\"FC Bayern München\",\"shortName\":\"Bayern\",\"crest\":null},"
            + "\"awayTeam\":{\"name\":\"Borussia Dortmund\",\"shortName\":\"Dortmund\",\"crest\":null},"
            + "\"result\":{\"home\":3,\"away\":1},\"myTip\":{\"home\":2,\"away\":0},\"myPoints\":3}]}";

    private static final String MATCHDAY_QUERY = "{ matchday(groupId: 3%s) { matchday serverTime matches { id kickoff status locked "
            + "homeTeam { name shortName crest } awayTeam { name } result { home away } myTip { home away } myPoints } } }";

    @Test
    void matchdayWithoutNumberAsksTheCoreWithoutQueryParameter() {
        core.reply("GET /api/groups/3/matches", 200, MATCHDAY_JSON);

        var result = graphql.post(String.format(MATCHDAY_QUERY, ""), BEARER);

        assertEquals(Integer.valueOf(6), result.get("$.data.matchday.matchday"));
        assertEquals("2026-10-17T12:00:00Z", result.get("$.data.matchday.serverTime"));
        assertNull(core.calls().get(0).query());
        assertEquals(BEARER, core.calls().get(0).authorization());
    }

    @Test
    void matchdayWithNumberPassesItAsQueryParameter() {
        core.reply("GET /api/groups/3/matches", 200, MATCHDAY_JSON);

        graphql.post(String.format(MATCHDAY_QUERY, ", matchday: 6"), BEARER);

        assertEquals("matchday=6", core.calls().get(0).query());
    }

    @Test
    void matchEntriesKeepTeamsLockOwnTipAndPoints() {
        core.reply("GET /api/groups/3/matches", 200, MATCHDAY_JSON);

        var result = graphql.post(String.format(MATCHDAY_QUERY, ""), BEARER);

        assertEquals("101", result.get("$.data.matchday.matches[0].id"));
        assertEquals("UPCOMING", result.get("$.data.matchday.matches[0].status"));
        assertEquals(false, result.get("$.data.matchday.matches[0].locked"));
        assertEquals("SC Freiburg", result.get("$.data.matchday.matches[0].homeTeam.name"));
        assertEquals("Freiburg", result.get("$.data.matchday.matches[0].homeTeam.shortName"));
        assertNull(result.get("$.data.matchday.matches[0].result"));
        assertEquals(Integer.valueOf(2), result.get("$.data.matchday.matches[0].myTip.home"));
        assertNull(result.get("$.data.matchday.matches[0].myPoints"));
        assertEquals(true, result.get("$.data.matchday.matches[1].locked"));
        assertEquals(Integer.valueOf(3), result.get("$.data.matchday.matches[1].result.home"));
        assertEquals(Integer.valueOf(3), result.get("$.data.matchday.matches[1].myPoints"));
    }

    @Test
    void matchdayOutOfRangeGivesTheCoreValidationError() {
        core.reply("GET /api/groups/3/matches", 400,
                "{\"error\":{\"code\":\"VALIDATION_ERROR\",\"message\":\"Der Spieltag muss zwischen 1 und 34 liegen.\"}}");

        var result = graphql.post(String.format(MATCHDAY_QUERY, ", matchday: 35"), BEARER);

        assertEquals("VALIDATION_ERROR", result.get("$.errors[0].extensions.code"));
        assertEquals("BAD_REQUEST", result.get("$.errors[0].extensions.classification"));
    }

    @Test
    void matchdayOfAForeignGroupGivesNotAMember() {
        core.reply("GET /api/groups/3/matches", 403,
                "{\"error\":{\"code\":\"NOT_A_MEMBER\",\"message\":\"Du bist kein Mitglied dieser Tippgruppe.\"}}");

        var result = graphql.post(String.format(MATCHDAY_QUERY, ""), BEARER);

        assertEquals("NOT_A_MEMBER", result.get("$.errors[0].extensions.code"));
    }

    @Test
    void rankingReturnsTheListWithTiesAndExactTips() {
        core.reply("GET /api/groups/3", 200, GROUP_JSON);
        core.reply("GET /api/groups/3/ranking", 200,
                "{\"groupId\":3,\"matchday\":null,\"ranking\":["
                        + "{\"rank\":1,\"userId\":12,\"username\":\"maximilian\",\"points\":35,\"exactTips\":4},"
                        + "{\"rank\":2,\"userId\":7,\"username\":\"niklas\",\"points\":31,\"exactTips\":3},"
                        + "{\"rank\":2,\"userId\":21,\"username\":\"max\",\"points\":31,\"exactTips\":2},"
                        + "{\"rank\":4,\"userId\":30,\"username\":\"arlind\",\"points\":27,\"exactTips\":2}]}");

        var result = graphql.post("{ ranking(groupId: 3) { rank userId username points exactTips } }", BEARER);

        List<?> ranking = result.get("$.data.ranking");
        assertEquals(4, ranking.size());
        assertEquals(Integer.valueOf(1), result.get("$.data.ranking[0].rank"));
        assertEquals("12", result.get("$.data.ranking[0].userId"));
        assertEquals("maximilian", result.get("$.data.ranking[0].username"));
        assertEquals(Integer.valueOf(2), result.get("$.data.ranking[1].rank"));
        assertEquals(Integer.valueOf(2), result.get("$.data.ranking[2].rank"));
        assertEquals(Integer.valueOf(4), result.get("$.data.ranking[3].rank"));
        assertNull(core.calls().get(1).query());
    }

    @Test
    void rankingForAMatchdayPassesTheParameter() {
        core.reply("GET /api/groups/3", 200, GROUP_JSON);
        core.reply("GET /api/groups/3/ranking", 200, "{\"groupId\":3,\"matchday\":6,\"ranking\":[]}");

        var result = graphql.post("{ ranking(groupId: 3, matchday: 6) { rank } }", BEARER);

        assertEquals("matchday=6", core.calls().get(1).query());
        assertEquals(0, ((List<?>) result.get("$.data.ranking")).size());
    }

    @Test
    void rankingOfAForeignGroupGivesNotAMember() {
        core.reply("GET /api/groups/3", 403,
                "{\"error\":{\"code\":\"NOT_A_MEMBER\",\"message\":\"Du bist kein Mitglied dieser Tippgruppe.\"}}");

        var result = graphql.post("{ ranking(groupId: 3) { rank } }", BEARER);

        assertEquals("NOT_A_MEMBER", result.get("$.errors[0].extensions.code"));
        assertTrue(result.has("$.errors"));
    }

    @Test
    void groupIdWithoutNumberIsRejectedBeforeTheCoreIsCalled() {
        var result = graphql.post("{ ranking(groupId: \"abc\") { rank } }", BEARER);

        assertEquals("VALIDATION_ERROR", result.get("$.errors[0].extensions.code"));
        assertTrue(core.calls().isEmpty());
    }
}
