package com.mosbach.tipprunde.graphql.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.mosbach.tipprunde.graphql.core.CoreClient;
import com.mosbach.tipprunde.graphql.core.Ids;
import com.mosbach.tipprunde.graphql.model.Group;
import com.mosbach.tipprunde.graphql.model.GroupSummary;
import com.mosbach.tipprunde.graphql.model.InvitePreview;

import reactor.core.publisher.Mono;

// Gruppen: myGroups, group, invitePreview. Mitgliedschaft und Rolle prüft allein der Core.
@Controller
public class GroupQueries {

    private static final String AUTH = AuthorizationHeaderInterceptor.CONTEXT_KEY;

    private final CoreClient core;

    public GroupQueries(CoreClient core) {
        this.core = core;
    }

    @QueryMapping
    public Mono<List<GroupSummary>> myGroups(@ContextValue(name = AUTH, required = false) String authorization) {
        return core.myGroups(authorization);
    }

    @QueryMapping
    public Mono<Group> group(@Argument String id, @ContextValue(name = AUTH, required = false) String authorization) {
        return core.group(authorization, Ids.requireNumber(id, "Die Gruppen-ID"));
    }

    @QueryMapping
    public Mono<InvitePreview> invitePreview(@Argument String code) {
        return core.invitePreview(code);
    }
}
