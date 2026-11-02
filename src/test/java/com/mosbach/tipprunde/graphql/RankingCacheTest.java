package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mosbach.tipprunde.graphql.core.RankingCache;
import com.mosbach.tipprunde.graphql.model.RankingEntry;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;

// Gültigkeit, Schlüssel und Log des Caches, ohne Core und ohne zu warten (eigene Uhr)
class RankingCacheTest {

    private static final List<RankingEntry> RANKING = List.of(new RankingEntry(1, 7, "niklas", 31, 3));

    private final FakeTicker ticker = new FakeTicker();
    private final RankingCache cache = new RankingCache(60, 100, ticker);
    private final AtomicInteger loads = new AtomicInteger();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RankingCache.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attach() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    private Mono<List<RankingEntry>> load() {
        return Mono.fromSupplier(() -> {
            loads.incrementAndGet();
            return RANKING;
        });
    }

    private List<String> log() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void secondCallWithinSixtySecondsIsServedFromTheCache() {
        cache.get(3, null, this::load).block();
        ticker.advance(Duration.ofSeconds(59));
        var second = cache.get(3, null, this::load).block();

        assertEquals(1, loads.get());
        assertEquals(RANKING, second);
    }

    @Test
    void callAfterSixtySecondsLoadsAgain() {
        cache.get(3, null, this::load).block();
        ticker.advance(Duration.ofSeconds(61));
        cache.get(3, null, this::load).block();

        assertEquals(2, loads.get());
    }

    @Test
    void groupAndMatchdayAreSeparateKeys() {
        cache.get(3, null, this::load).block();
        cache.get(3, 6, this::load).block();
        cache.get(4, null, this::load).block();
        cache.get(3, 6, this::load).block();

        assertEquals(3, loads.get());
    }

    @Test
    void hitsAndMissesAreLoggedWithTheKeyOnly() {
        cache.get(3, null, this::load).block();
        cache.get(3, null, this::load).block();
        cache.get(3, 6, this::load).block();

        assertEquals(List.of("Cache miss: key=ranking:3:total", "Cache hit: key=ranking:3:total",
                "Cache miss: key=ranking:3:6"), log());
    }

    @Test
    void failuresAreNotCached() {
        assertThrows(IllegalStateException.class,
                () -> cache.get(3, null, () -> Mono.<List<RankingEntry>>error(new IllegalStateException("Core weg"))).block());

        cache.get(3, null, this::load).block();

        assertEquals(1, loads.get());
        assertTrue(log().stream().allMatch(line -> line.contains("miss")), log().toString());
    }

    @Test
    void clearEmptiesTheCache() {
        cache.get(3, null, this::load).block();
        cache.clear();
        cache.get(3, null, this::load).block();

        assertEquals(2, loads.get());
    }

    private static class FakeTicker implements Ticker {

        private long nanos;

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long read() {
            return nanos;
        }
    }
}
