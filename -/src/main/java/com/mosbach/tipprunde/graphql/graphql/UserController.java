package com.mosbach.tipprunde.graphql.graphql;

import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.mosbach.tipprunde.graphql.core.CoreClient;
import com.mosbach.tipprunde.graphql.model.User;

import reactor.core.publisher.Mono;

// Eigenes Profil: me
@Controller
public class UserController {

    private final CoreClient core;

    public UserController(CoreClient core) {
        this.core = core;
    }

    @QueryMapping
    public Mono<User> me(@ContextValue(name = AuthorizationHeaderInterceptor.CONTEXT_KEY, required = false) String authorization) {
        return core.me(authorization);
    }
}
