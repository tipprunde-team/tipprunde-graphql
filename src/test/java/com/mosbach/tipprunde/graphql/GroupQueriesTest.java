package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class GroupQueriesTest extends AbstractGraphqlTest {

    private static final String BEARER = "Bearer abc.def.ghi";

    @Test
    void myGroupsReturnsTheListWithRankAndPoints() {
        core.reply("GET /api/groups", 200,
                "[{\"id\":3,\"name\":\"WG Lohrtalweg\",\"role\":\"ADMIN\",\"memberCount\":4,\"myRank\":2,\"myPoints\":31},"
                        + "{\"id\":5,\"name\":\"Büro\",\"role\":\"MEMBER\",\"memberCount\":12,\"myRank\":1,\"myPoints\":9}]");

        var result = graphql.post("{ myGroups { id name role memberCount myRank myPoints } }", BEARER);

        assertEquals(2, ((List<?>) result.get("$.data.myGroups")).size());
        assertEquals("3", result.get("$.data.myGroups[0].id"));
        assertEquals("WG Lohrtalweg", result.get("$.data.myGroups[0].name"));
        assertEquals("ADMIN", result.get("$.data.myGroups[0].role"));
        assertEquals(Integer.valueOf(2), result.get("$.data.myGroups[0].myRank"));
        assertEquals("Büro", result.get("$.data.myGroups[1].name"));
        assertEquals(BEARER, core.calls().get(0).authorization());
    }

    @Test
    void myGroupsWithoutGroupsIsAnEmptyList() {
        core.reply("GET /api/groups", 200, "[]");

        var result = graphql.post("{ myGroups { id } }", BEARER);

        assertEquals(0, ((List<?>) result.get("$.data.myGroups")).size());
    }

    @Test
    void myGroupsWithoutTokenGivesUnauthorized() {
        core.reply("GET /api/groups", 401, "{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Bitte melde dich erneut an.\"}}");

        var result = graphql.post("{ myGroups { id } }", null);

        assertEquals("UNAUTHORIZED", result.get("$.errors[0].extensions.code"));
        assertNull(core.calls().get(0).authorization());
    }

    @Test
    void groupDetailsForAnAdminContainMembersAndInviteCode() {
        core.reply("GET /api/groups/3", 200,
                "{\"id\":3,\"name\":\"WG Lohrtalweg\",\"inviteCode\":\"k7Qp2xZ9aB\",\"members\":["
                        + "{\"userId\":7,\"username\":\"niklas\",\"role\":\"ADMIN\",\"joinedAt\":\"2026-10-01T18:00:00Z\"},"
                        + "{\"userId\":12,\"username\":\"maximilian\",\"role\":\"MEMBER\",\"joinedAt\":\"2026-10-01T18:05:00Z\"}]}");

        var result = graphql.post("{ group(id: 3) { id name inviteCode members { userId username role joinedAt } } }", BEARER);

        assertEquals("3", result.get("$.data.group.id"));
        assertEquals("k7Qp2xZ9aB", result.get("$.data.group.inviteCode"));
        assertEquals("niklas", result.get("$.data.group.members[0].username"));
        assertEquals("2026-10-01T18:05:00Z", result.get("$.data.group.members[1].joinedAt"));
    }

    @Test
    void groupDetailsForAMemberHaveNoInviteCode() {
        core.reply("GET /api/groups/3", 200,
                "{\"id\":3,\"name\":\"WG Lohrtalweg\",\"members\":[{\"userId\":7,\"username\":\"niklas\",\"role\":\"ADMIN\",\"joinedAt\":\"2026-10-01T18:00:00Z\"}]}");

        var result = graphql.post("{ group(id: 3) { name inviteCode } }", BEARER);

        assertEquals("WG Lohrtalweg", result.get("$.data.group.name"));
        assertNull(result.get("$.data.group.inviteCode"));
        assertFalse(result.has("$.errors"));
    }

    @Test
    void foreignGroupGivesNotAMemberWithTheCoreMessage() {
        core.reply("GET /api/groups/3", 403,
                "{\"error\":{\"code\":\"NOT_A_MEMBER\",\"message\":\"Du bist kein Mitglied dieser Tippgruppe.\"}}");

        var result = graphql.post("{ group(id: 3) { id } }", BEARER);

        assertEquals("NOT_A_MEMBER", result.get("$.errors[0].extensions.code"));
        assertEquals("FORBIDDEN", result.get("$.errors[0].extensions.classification"));
        assertEquals("Du bist kein Mitglied dieser Tippgruppe.", result.get("$.errors[0].message"));
    }

    @Test
    void unknownGroupGivesGroupNotFound() {
        core.reply("GET /api/groups/999", 404,
                "{\"error\":{\"code\":\"GROUP_NOT_FOUND\",\"message\":\"Diese Tippgruppe gibt es nicht.\"}}");

        var result = graphql.post("{ group(id: 999) { id } }", BEARER);

        assertEquals("GROUP_NOT_FOUND", result.get("$.errors[0].extensions.code"));
        assertEquals("NOT_FOUND", result.get("$.errors[0].extensions.classification"));
    }

    @Test
    void groupIdThatIsNotANumberNeverReachesTheCore() {
        var result = graphql.post("{ group(id: \"3/../../users/me\") { id } }", BEARER);

        assertEquals("VALIDATION_ERROR", result.get("$.errors[0].extensions.code"));
        assertTrue(core.calls().isEmpty());
    }

    @Test
    void invitePreviewWorksWithoutTokenAndSendsNoAuthorizationHeader() {
        core.reply("GET /api/invites/k7Qp2xZ9aB", 200, "{\"groupId\":3,\"groupName\":\"WG Lohrtalweg\",\"memberCount\":4}");

        var result = graphql.post("{ invitePreview(code: \"k7Qp2xZ9aB\") { groupId groupName memberCount } }", BEARER);

        assertEquals("3", result.get("$.data.invitePreview.groupId"));
        assertEquals("WG Lohrtalweg", result.get("$.data.invitePreview.groupName"));
        assertEquals(Integer.valueOf(4), result.get("$.data.invitePreview.memberCount"));
        assertNull(core.calls().get(0).authorization());
    }

    @Test
    void invalidInviteCodeGivesInviteInvalid() {
        core.reply("GET /api/invites/falsch", 404,
                "{\"error\":{\"code\":\"INVITE_INVALID\",\"message\":\"Dieser Einladungscode ist ungültig.\"}}");

        var result = graphql.post("{ invitePreview(code: \"falsch\") { groupId } }", null);

        assertEquals("INVITE_INVALID", result.get("$.errors[0].extensions.code"));
    }

    @Test
    void inviteCodeWithSlashesIsEncodedAndStaysInsideTheInvitePath() {
        graphql.post("{ invitePreview(code: \"../groups\") { groupId } }", null);

        assertEquals(1, core.calls().size());
        assertTrue(core.calls().get(0).path().startsWith("/api/invites/"), core.calls().get(0).path());
    }
}
