package com.djrapitops.plan.domain.gathering.events;

import com.djrapitops.plan.domain.DataService;
import com.djrapitops.plan.domain.Mapping;
import com.djrapitops.plan.domain.delivery.FinishedSession;
import com.djrapitops.plan.domain.delivery.ServerUUID;
import com.djrapitops.plan.domain.gathering.ActiveSession;
import com.djrapitops.plan.gathering.domain.WorldTimes;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventMapping implements Mapping {

    private final DataService dataService;

    @Inject
    public EventMapping(DataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void register() {
        dataService.registerMapper(
                PlayerServerJoin.class, WorldTimes.class,
                this::mapToNewWorldTimes
        ).registerMapper(
                PlayerServerJoin.class, ActiveSession.class,
                this::mapToActiveSession
        ).registerMapper(
                PlayerLeave.class, FinishedSession.class,
                this::mapToFinishedSession
        );
    }

    private WorldTimes mapToNewWorldTimes(PlayerServerJoin event) {
        return new WorldTimes(
                event.information.worldName,
                event.information.gamemode.name(),
                event.time
        );
    }

    private ActiveSession mapToActiveSession(PlayerServerJoin event) {
        return new ActiveSession(
                event.information.playerUUID,
                event.time,
                dataService.mapTo(WorldTimes.class, event)
        );
    }

    private FinishedSession mapToFinishedSession(PlayerLeave leave) {
        ServerUUID serverUUID = dataService.pull(ServerUUID.class)
                .orElseThrow(IllegalStateException::new);
        return dataService.pull(ActiveSession.class)
                .map(session -> session.toFinishedSession(serverUUID, leave.time))
                .orElse(null);
    }
}
