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
package com.djrapitops.plan.domain.gathering;

import java.net.InetAddress;
import java.util.UUID;

public class PlayerInformation {
    public final UUID playerUUID;
    public final String playerName;
    public final String displayName;
    public final InetAddress ipAddress;
    public final String worldName;
    public final Gamemode gamemode;

    public PlayerInformation(
            UUID playerUUID,
            String playerName,
            String displayName,
            InetAddress ipAddress,
            String worldName,
            Gamemode gamemode
    ) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.displayName = displayName;
        this.ipAddress = ipAddress;
        this.worldName = worldName;
        this.gamemode = gamemode;
    }
}
