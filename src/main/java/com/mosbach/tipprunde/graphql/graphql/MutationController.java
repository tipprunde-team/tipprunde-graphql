package com.mosbach.tipprunde.graphql.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.mosbach.tipprunde.graphql.core.CoreClient;
import com.mosbach.tipprunde.graphql.model.Group;
import com.mosbach.tipprunde.graphql.model.GroupSummary;
import com.mosbach.tipprunde.graphql.model.TokenAnswer;
import com.mosbach.tipprunde.graphql.model.User;

import reactor.core.publisher.Mono;

// Mutationen: alle Regeln (Validierung, Sperre, Mitgliedschaft) bleiben im Core, hier wird nur weitergereicht
@Controller
public class MutationController {

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
}
