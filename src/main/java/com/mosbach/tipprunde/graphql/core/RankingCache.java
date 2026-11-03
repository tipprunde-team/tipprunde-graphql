package com.mosbach.tipprunde.graphql.core;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.mosbach.tipprunde.graphql.model.RankingEntry;

import reactor.core.publisher.Mono;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// Cache für die Rangliste einer Gruppe. Gecacht werden nur nutzerunabhängige Daten (die Rangliste ist für alle Mitglieder gleich),
// der Schlüssel ist Gruppe und Spieltag. Eigene Tipps werden nie gecacht. Treffer und Fehlschläge stehen im Log.
@Component
public class RankingCache {

    private static final Logger log = LoggerFactory.getLogger(RankingCache.class);

    private final Cache<String, List<RankingEntry>> cache;

    @Autowired
    public RankingCache(@Value("${app.cache.ttl-seconds}") long ttlSeconds, @Value("${app.cache.max-size}") long maxSize) {
        this(ttlSeconds, maxSize, Ticker.systemTicker());
    }

    // Mit eigener Uhr, damit Tests die Gültigkeit prüfen können, ohne zu warten
    public RankingCache(long ttlSeconds, long maxSize, Ticker ticker) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxSize)
                .ticker(ticker)
                .build();
    }

    // Der Aufrufer prüft vorher Token und Mitgliedschaft. Fehler des Laders werden nicht gecacht.
    public Mono<List<RankingEntry>> get(long groupId, Integer matchday, Supplier<Mono<List<RankingEntry>>> loader) {
        String key = key(groupId, matchday);
        List<RankingEntry> cached = cache.getIfPresent(key);
        if (cached != null) {
            log.info("Cache hit: key={}", key);
            return Mono.just(cached);
        }
        log.info("Cache miss: key={}", key);
        return loader.get().doOnNext(ranking -> cache.put(key, ranking));
    }

    // Leert den Cache, die Tests brauchen für jeden Fall einen leeren Zustand
    public void clear() {
        cache.invalidateAll();
    }

    static String key(long groupId, Integer matchday) {
        return "ranking:" + groupId + ":" + (matchday == null ? "total" : matchday);
    }
}
