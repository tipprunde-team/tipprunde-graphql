package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

// Der Header Authorization geht unverändert zum Core, Fehler des Core kommen mit demselben code zurück
class MeQueryTest extends AbstractGraphqlTest {

    @Test
    void meReturnsTheProfileFromTheCore() {
        core.reply("GET /api/users/me", 200,
                "{\"id\":7,\"email\":\"niklas@example.de\",\"username\":\"niklas\",\"isSystemAdmin\":false}");

        var result = graphql.post("{ me { id username email isSystemAdmin } }", "Bearer abc.def.ghi");

        assertEquals("7", result.get("$.data.me.id"));
        assertEquals("niklas", result.get("$.data.me.username"));
        assertEquals("niklas@example.de", result.get("$.data.me.email"));
        assertEquals(false, result.get("$.data.me.isSystemAdmin"));
        assertFalse(result.has("$.errors"));
    }

    @Test
    void authorizationHeaderIsPassedToTheCoreUnchanged() {
        core.reply("GET /api/users/me", 200, "{\"id\":7,\"username\":\"niklas\"}");

        graphql.post("{ me { id } }", "Bearer abc.def.ghi");

        assertEquals("Bearer abc.def.ghi", core.calls().get(0).authorization());
    }

    @Test
    void withoutTokenNoAuthorizationHeaderIsSentAndTheCoreErrorCodeIsReturned() {
        core.reply("GET /api/users/me", 401,
                "{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Bitte melde dich erneut an.\"}}");

        var result = graphql.post("{ me { id } }", null);

        assertEquals(null, core.calls().get(0).authorization());
        assertEquals("UNAUTHORIZED", result.get("$.errors[0].extensions.code"));
        assertEquals("UNAUTHORIZED", result.get("$.errors[0].extensions.classification"));
        assertEquals("Bitte melde dich erneut an.", result.get("$.errors[0].message"));
    }

    @Test
    void invalidTokenGivesTheCoreErrorAsGraphqlError() {
        core.reply("GET /api/users/me", 401,
                "{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Bitte melde dich erneut an.\"}}");

        var result = graphql.post("{ me { id } }", "Bearer manipuliert");

        assertEquals("UNAUTHORIZED", result.get("$.errors[0].extensions.code"));
        assertEquals(200, result.status());
    }

    @Test
    void errorBodyWithoutTheCoreFormatBecomesAGenericCoreError() {
        core.reply("GET /api/users/me", 500, "kein json");

        var result = graphql.post("{ me { id } }", "Bearer x");

        assertEquals("CORE_ERROR", result.get("$.errors[0].extensions.code"));
    }
}
