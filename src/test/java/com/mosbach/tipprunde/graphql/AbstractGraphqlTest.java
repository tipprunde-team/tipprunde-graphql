package com.mosbach.tipprunde.graphql;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Startet den GraphQL-Service gegen einen gemeinsamen Platzhalter-Core. Alle Testklassen teilen sich einen Kontext.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractGraphqlTest {

    static final MockCore core = new MockCore();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(core::stop));
    }

    @DynamicPropertySource
    static void coreUrl(DynamicPropertyRegistry registry) {
        registry.add("app.core.base-url", core::baseUrl);
        registry.add("app.core.timeout-seconds", () -> 3);
    }

    @Autowired
    Environment environment;

    GraphqlTestSupport graphql;

    @BeforeEach
    void setUp() {
        core.reset();
        graphql = new GraphqlTestSupport(environment);
    }
}
