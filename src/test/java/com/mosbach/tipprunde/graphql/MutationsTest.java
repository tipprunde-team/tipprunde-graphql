package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

class MutationsTest extends AbstractGraphqlTest {

    private static final String BEARER = "Bearer abc.def.ghi";

    @Test
    void registerSendsTheBodyWithoutTokenAndReturnsTheUser() {
        core.reply("POST /api/users", 201, "{\"id\":9,\"email\":\"anna@example.de\",\"username\":\"anna\"}");

        var result = graphql.post("mutation { register(email: \"anna@example.de\", username: \"anna\", password: \"Geheim123\") "
                + "{ id email username } }", BEARER);

        assertEquals("9", result.get("$.data.register.id"));
        assertEquals("anna", result.get("$.data.register.username"));
        var call = core.calls().get(0);
        assertNull(call.authorization());
        assertEquals("anna@example.de", JsonPath.read(call.body(), "$.email"));
        assertEquals("Geheim123", JsonPath.read(call.body(), "$.password"));
    }

    @Test
    void registerWithTakenEmailGivesTheCoreCodeAsBadRequest() {
        core.reply("POST /api/users", 409,
                "{\"error\":{\"code\":\"EMAIL_TAKEN\",\"message\":\"Diese E-Mail-Adresse ist schon registriert.\"}}");

        var result = graphql.post("mutation { register(email: \"a@b.de\", username: \"anna\", password: \"Geheim123\") { id } }", null);

        assertEquals("EMAIL_TAKEN", result.get("$.errors[0].extensions.code"));
        assertEquals("BAD_REQUEST", result.get("$.errors[0].extensions.classification"));
    }

    @Test
    void loginReturnsTokenExpiryAndUser() {
        core.reply("POST /api/auth", 200,
                "{\"token\":\"eyJ.abc.def\",\"expiresAt\":\"2026-10-28T09:15:00Z\",\"user\":{\"id\":7,\"username\":\"niklas\"}}");

        var result = graphql.post("mutation { login(email: \"niklas@example.de\", password: \"Geheim123\") "
                + "{ token expiresAt user { id username } } }", null);

        assertEquals("eyJ.abc.def", result.get("$.data.login.token"));
        assertEquals("2026-10-28T09:15:00Z", result.get("$.data.login.expiresAt"));
        assertEquals("7", result.get("$.data.login.user.id"));
        assertEquals("niklas", result.get("$.data.login.user.username"));
    }

    @Test
    void wrongPasswordGivesTheSameUnauthorizedErrorAsTheCore() {
        core.reply("POST /api/auth", 401,
                "{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"E-Mail oder Passwort ist falsch.\"}}");

        var result = graphql.post("mutation { login(email: \"x@y.de\", password: \"falsch\") { token } }", null);

        assertEquals("UNAUTHORIZED", result.get("$.errors[0].extensions.code"));
        assertEquals("E-Mail oder Passwort ist falsch.", result.get("$.errors[0].message"));
    }

    @Test
    void createGroupSendsTheNameWithTokenAndReturnsCodeAndRole() {
        core.reply("POST /api/groups", 201, "{\"id\":3,\"name\":\"WG Test\",\"inviteCode\":\"k7Qp2xZ9aB\",\"role\":\"ADMIN\"}");

        var result = graphql.post("mutation { createGroup(name: \"WG Test\") { id name inviteCode role } }", BEARER);

        assertEquals("3", result.get("$.data.createGroup.id"));
        assertEquals("k7Qp2xZ9aB", result.get("$.data.createGroup.inviteCode"));
        assertEquals("ADMIN", result.get("$.data.createGroup.role"));
        assertEquals(BEARER, core.calls().get(0).authorization());
        assertEquals("WG Test", JsonPath.read(core.calls().get(0).body(), "$.name"));
    }

    @Test
    void joinGroupReturnsTheGroupEntry() {
        core.reply("POST /api/groups/join", 200,
                "{\"id\":3,\"name\":\"WG Test\",\"role\":\"MEMBER\",\"memberCount\":5,\"myRank\":5,\"myPoints\":0}");

        var result = graphql.post("mutation { joinGroup(inviteCode: \"k7Qp2xZ9aB\") { id role memberCount } }", BEARER);

        assertEquals("MEMBER", result.get("$.data.joinGroup.role"));
        assertEquals(Integer.valueOf(5), result.get("$.data.joinGroup.memberCount"));
        assertEquals("k7Qp2xZ9aB", JsonPath.read(core.calls().get(0).body(), "$.inviteCode"));
    }

    @Test
    void joinGroupWhenAlreadyMemberGivesTheCoreCode() {
        core.reply("POST /api/groups/join", 409,
                "{\"error\":{\"code\":\"ALREADY_MEMBER\",\"message\":\"Du bist schon Mitglied dieser Tippgruppe.\"}}");

        var result = graphql.post("mutation { joinGroup(inviteCode: \"k7Qp2xZ9aB\") { id } }", BEARER);

        assertEquals("ALREADY_MEMBER", result.get("$.errors[0].extensions.code"));
    }
}
