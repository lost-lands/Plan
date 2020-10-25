/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.gathering.sessions;

import com.djrapitops.plan.delivery.domain.keys.SessionKeys;
import com.djrapitops.plan.domain.DataService;
import com.djrapitops.plan.domain.delivery.ServerUUID;
import com.djrapitops.plan.domain.gathering.events.PlayerLeave;
import com.djrapitops.plan.domain.gathering.events.PlayerServerJoin;
import com.djrapitops.plan.gathering.domain.Session;
import com.djrapitops.plan.gathering.domain.WorldTimes;
import com.djrapitops.plan.gathering.domain.sessions.ActiveSession;
import com.djrapitops.plan.gathering.domain.sessions.FinishedSession;
import com.djrapitops.plan.gathering.domain.sessions.UnfinishedSessions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to store active sessions of players in memory.
 *
 * @author Rsl1122
 */
@Singleton
public class SessionCache {

    private static final Map<UUID, Session> ACTIVE_SESSIONS_old = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private final DataService dataService;

    @Inject
    public SessionCache(
            DataService dataService
    ) {
        // Dagger requires empty inject constructor
        this.dataService = dataService;

        dataService.registerMapper(PlayerServerJoin.class, WorldTimes.class, this::mapToNewWorldTimes)
                .registerMapper(PlayerServerJoin.class, ActiveSession.class, this::startSession)
                .registerConsumer(ActiveSession.class, this::cacheSession)
                .registerSupplier(ActiveSession.class, UUID.class, ACTIVE_SESSIONS::get)
                .registerMapper(PlayerLeave.class, FinishedSession.class, this::finishSession)
                .registerSupplier(UnfinishedSessions.class, () -> new UnfinishedSessions(ACTIVE_SESSIONS.values()));
    }

    @Deprecated
    public static Map<UUID, Session> getActiveSessions() {
        refreshActiveSessionsState();
        return Collections.unmodifiableMap(new HashMap<>(ACTIVE_SESSIONS_old));
    }

    @Deprecated
    public static void clear() {
        ACTIVE_SESSIONS_old.clear();
    }

    @Deprecated
    public static void refreshActiveSessionsState() {
        ACTIVE_SESSIONS_old.values().forEach(Session::updateState);
    }

    /**
     * Used to get the Session of the player in the sessionCache.
     *
     * @param playerUUID UUID of the player.
     * @return Optional with the session inside it if found.
     */
    @Deprecated
    public static Optional<Session> getCachedSession(UUID playerUUID) {
        Optional<Session> found = Optional.ofNullable(ACTIVE_SESSIONS_old.get(playerUUID));
        found.ifPresent(Session::updateState);
        return found;
    }

    private WorldTimes mapToNewWorldTimes(PlayerServerJoin event) {
        return new WorldTimes(
                event.information.worldName, event.information.gamemode.name(), event.time
        );
    }

    private ActiveSession startSession(PlayerServerJoin event) {
        return new ActiveSession(
                event.information.playerUUID, event.time, dataService.mapTo(WorldTimes.class, event)
        );
    }

    private FinishedSession finishSession(PlayerLeave leave) {
        ServerUUID serverUUID = dataService.pull(ServerUUID.class)
                .orElseThrow(IllegalStateException::new);
        return dataService.pull(ActiveSession.class)
                .map(session -> {
                    ACTIVE_SESSIONS.remove(session.playerUUID);
                    return session.toFinishedSession(serverUUID, leave.time);
                }).orElse(null);
    }

    private void cacheSession(ActiveSession newSession) {
        ActiveSession activeSession = ACTIVE_SESSIONS.get(newSession.playerUUID);
        if (activeSession != null) {
            dataService.push(
                    FinishedSession.class,
                    activeSession.toFinishedSession(
                            dataService.pull(ServerUUID.class).orElseThrow(UnsupportedOperationException::new),
                            newSession.start
                    )
            );
        }
        ACTIVE_SESSIONS.put(newSession.playerUUID, newSession);
    }

    /**
     * Cache a new session.
     *
     * @param playerUUID UUID of the player
     * @param session    Session to cache.
     * @return Optional: previous session. Recipients of this object should decide if it needs to be saved.
     */
    @Deprecated
    public Optional<Session> cacheSession(UUID playerUUID, Session session) {
        Optional<Session> inProgress = Optional.empty();
        if (getCachedSession(playerUUID).isPresent()) {
            inProgress = endSession(playerUUID, session.getUnsafe(SessionKeys.START));
        }
        ACTIVE_SESSIONS_old.put(playerUUID, session);
        return inProgress;
    }

    /**
     * End a session and save it to database.
     *
     * @param playerUUID UUID of the player.
     * @param time       Time the session ended.
     * @return Optional: ended session. Recipients of this object should decide if it needs to be saved.
     */
    @Deprecated
    public Optional<Session> endSession(UUID playerUUID, long time) {
        Session session = ACTIVE_SESSIONS_old.get(playerUUID);
        if (session == null || session.getUnsafe(SessionKeys.START) > time) {
            return Optional.empty();
        }
        ACTIVE_SESSIONS_old.remove(playerUUID);
        session.endSession(time);
        return Optional.of(session);
    }
}
