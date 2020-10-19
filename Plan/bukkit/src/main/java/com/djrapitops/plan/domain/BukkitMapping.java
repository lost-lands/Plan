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
package com.djrapitops.plan.domain;

import com.djrapitops.plan.domain.gathering.Gamemode;
import com.djrapitops.plan.domain.gathering.PlayerInformation;
import com.djrapitops.plan.domain.gathering.PlayerServerJoin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BukkitMapping implements Mapping {

    private final DataService dataService;

    @Inject
    public BukkitMapping(DataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void register() {
        dataService.registerMapper(
                Player.class, PlayerInformation.class,
                player -> new PlayerInformation(
                        player.getUniqueId(),
                        player.getName(),
                        player.getDisplayName(),
                        player.getAddress().getAddress(),
                        player.getWorld().getName(),
                        dataService.mapTo(Gamemode.class, player.getGameMode())
                )
        ).registerMapper(
                GameMode.class, Gamemode.class,
                gm -> Gamemode.valueOf(gm.name())
        ).registerMapper(
                PlayerJoinEvent.class, PlayerServerJoin.class,
                event -> new PlayerServerJoin(
                        dataService.mapTo(PlayerInformation.class, event.getPlayer()),
                        System.currentTimeMillis()
                )
        );
    }
}
