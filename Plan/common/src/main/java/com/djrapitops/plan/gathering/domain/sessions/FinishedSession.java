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

import java.util.List;
import java.util.UUID;

public class FinishedSession {
    public final UUID playerUUID;
    public final ServerUUID serverUUID;
    public final long start;
    public final long end;
    public final List<PlayerKill> kills;
    public final WorldTimes worldTimes;
    public final long afkTime;

    public FinishedSession(
            UUID playerUUID,
            ServerUUID serverUUID,
            long start,
            long end,
            List<PlayerKill> kills,
            WorldTimes worldTimes,
            long afkTime
    ) {
        this.playerUUID = playerUUID;
        this.serverUUID = serverUUID;
        this.start = start;
        this.end = end;
        this.kills = kills;
        this.worldTimes = worldTimes;
        this.afkTime = afkTime;
    }
}
