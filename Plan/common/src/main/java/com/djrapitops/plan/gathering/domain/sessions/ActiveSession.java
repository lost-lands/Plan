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
package com.djrapitops.plan.gathering.domain.sessions;

import com.djrapitops.plan.domain.delivery.ServerUUID;
import com.djrapitops.plan.gathering.domain.WorldTimes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActiveSession {
    public final UUID playerUUID;
    public final long start;
    public final List<PlayerKill> kills;
    public final WorldTimes worldTimes;
    private long afkTime;

    public ActiveSession(
            UUID playerUUID,
            long start,
            WorldTimes worldTimes
    ) {
        this.playerUUID = playerUUID;
        this.start = start;
        this.worldTimes = worldTimes;
        this.kills = new ArrayList<>();
        afkTime = 0L;
    }

    public void addAfkTime(long amount) {
        afkTime += amount;
    }

    public FinishedSession toFinishedSession(ServerUUID serverUUID, long end) {
        return new FinishedSession(
                playerUUID,
                serverUUID,
                start,
                end,
                kills,
                worldTimes,
                afkTime
        );
    }
}
