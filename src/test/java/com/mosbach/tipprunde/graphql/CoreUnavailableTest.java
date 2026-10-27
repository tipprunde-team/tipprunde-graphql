package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Core nicht erreichbar (Port 1 ist geschlossen): klare Fehlermeldung statt interner Details
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoreUnavailableTest {

    @DynamicPropertySource
    static void coreUrl(DynamicPropertyRegistry registry) {
        registry.add("app.core.base-url", () -> "http://127.0.0.1:1");
        registry.add("app.core.timeout-seconds", () -> 3);
    }

    @Autowired
    Environment environment;

    @Test
    void unreachableCoreGivesCoreUnavailableWithoutInternalDetails() {
        var result = new GraphqlTestSupport(environment).post("{ me { id } }", "Bearer x");

        assertEquals("CORE_UNAVAILABLE", result.get("$.errors[0].extensions.code"));
        String message = result.get("$.errors[0].message");
        assertTrue(message.contains("nicht erreichbar"), message);
        assertFalse(message.contains("127.0.0.1"), message);
    }
}
