package com.mosbach.tipprunde.graphql.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.mosbach.tipprunde.graphql.core.CoreClient;
import com.mosbach.tipprunde.graphql.core.Ids;
import com.mosbach.tipprunde.graphql.core.RankingCache;
import com.mosbach.tipprunde.graphql.model.Matchday;
import com.mosbach.tipprunde.graphql.model.RankingEntry;

import reactor.core.publisher.Mono;

// Spieltag und Rangliste einer Gruppe. Der Spieltag ist ohne Angabe der aktuelle und wird nicht gecacht, weil er den eigenen Tipp enthält.
@Controller
public class MatchQueries {

    private static final String AUTH = AuthorizationHeaderInterceptor.CONTEXT_KEY;

    private final CoreClient core;
    private final RankingCache rankingCache;

    public MatchQueries(CoreClient core, RankingCache rankingCache) {
        this.core = core;
        this.rankingCache = rankingCache;
    }

    @QueryMapping
    public Mono<Matchday> matchday(@Argument String groupId, @Argument Integer matchday,
            @ContextValue(name = AUTH, required = false) String authorization) {
        return core.matchday(authorization, Ids.requireNumber(groupId, "Die Gruppen-ID"), matchday);
    }

    @QueryMapping
    public Mono<List<RankingEntry>> ranking(@Argument String groupId, @Argument Integer matchday,
            @ContextValue(name = AUTH, required = false) String authorization) {
        long id = Ids.requireNumber(groupId, "Die Gruppen-ID");
        // Token und Mitgliedschaft werden vor jedem Cache-Zugriff über den Core geprüft: bei 401, 403 oder 404 kommt der Fehler
        // zurück, und weder eine gecachte noch eine neue Rangliste wird ausgeliefert
        return core.group(authorization, id)
                .then(Mono.defer(() -> rankingCache.get(id, matchday, () -> core.ranking(authorization, id, matchday))));
    }
}
