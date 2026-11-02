package com.mosbach.tipprunde.graphql.graphql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.mosbach.tipprunde.graphql.core.CoreClient;
import com.mosbach.tipprunde.graphql.core.Ids;
import com.mosbach.tipprunde.graphql.model.Group;
import com.mosbach.tipprunde.graphql.model.GroupSummary;
import com.mosbach.tipprunde.graphql.model.Tip;
import com.mosbach.tipprunde.graphql.model.TokenAnswer;
import com.mosbach.tipprunde.graphql.model.User;

import reactor.core.publisher.Mono;

// Mutationen: alle Regeln (Validierung, Sperre, Mitgliedschaft) bleiben im Core, hier wird nur weitergereicht
@Controller
public class MutationController {

    private static final Logger log = LoggerFactory.getLogger(MutationController.class);
    private static final String AUTH = AuthorizationHeaderInterceptor.CONTEXT_KEY;

    private final CoreClient core;

    public MutationController(CoreClient core) {
        this.core = core;
    }

    @MutationMapping
    public Mono<User> register(@Argument String email, @Argument String username, @Argument String password) {
        return core.register(email, username, password);
    }

    @MutationMapping
    public Mono<TokenAnswer> login(@Argument String email, @Argument String password) {
        return core.login(email, password);
    }

    @MutationMapping
    public Mono<Group> createGroup(@Argument String name,
            @ContextValue(name = AUTH, required = false) String authorization) {
        return core.createGroup(authorization, name);
    }

    @MutationMapping
    public Mono<GroupSummary> joinGroup(@Argument String inviteCode,
            @ContextValue(name = AUTH, required = false) String authorization) {
        return core.joinGroup(authorization, inviteCode);
    }

    // Nie Passwort, Token oder Einladungscode ins Log, nur IDs
    @MutationMapping
    public Mono<Tip> submitTip(@Argument String groupId, @Argument String matchId, @Argument int home,
            @Argument int away, @ContextValue(name = AUTH, required = false) String authorization) {
        long group = Ids.requireNumber(groupId, "Die Gruppen-ID");
        long match = Ids.requireNumber(matchId, "Die Spiel-ID");
        log.info("Tip submitted: groupId={}, matchId={}", group, match);
        return core.submitTip(authorization, group, match, home, away);
    }
}
