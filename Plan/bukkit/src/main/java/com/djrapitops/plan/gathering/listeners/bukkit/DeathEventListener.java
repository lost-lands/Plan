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
package com.djrapitops.plan.gathering.listeners.bukkit;

import com.djrapitops.plan.delivery.formatting.EntityNameFormatter;
import com.djrapitops.plan.delivery.formatting.ItemNameFormatter;
import com.djrapitops.plan.gathering.cache.SessionCache;
import com.djrapitops.plan.gathering.domain.Session;
import com.djrapitops.plan.processing.Processing;
import com.djrapitops.plan.processing.processors.player.MobKillProcessor;
import com.djrapitops.plan.processing.processors.player.PlayerKillProcessor;
import com.djrapitops.plan.utilities.logging.ErrorContext;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import com.djrapitops.plugin.logging.L;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.entity.PlayerDeathEvent;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Event Listener for EntityDeathEvents.
 *
 * @author Rsl1122
 */
public class DeathEventListener implements Listener {

    private final Processing processing;
    private final ErrorLogger errorLogger;

    @Inject
    public DeathEventListener(
            Processing processing,
            ErrorLogger errorLogger
    ) {
        this.processing = processing;
        this.errorLogger = errorLogger;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if(event.getEntity() instanceof Player) {
            String deathmessage = event.getDeathMessage();
            if(deathmessage.contains("with an End Crystal")) { //Player was killed by an End Crystal
                if (event.getNewExp() < 0) {
                    //Mock crystal death event from DMPMonitorCombat
                    // DMPMonitorCombat sets the newExp to -1 so we can differentiate between mock and real death events
                    long time = System.currentTimeMillis();
                    String[] message = deathmessage.split("\\s+");
                    Player killer = Bukkit.getPlayer(ChatColor.stripColor(message[0]));
                    Player victim = event.getEntity();
                    Runnable processor = new PlayerKillProcessor(killer.getUniqueId(), time, victim.getUniqueId(), "End Crystal");
                    processing.submit(processor);
                }
            }
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        long time = System.currentTimeMillis();
        LivingEntity dead = event.getEntity();

        if (dead.getLastDamage() < 0) { 
        //checks if the last damage is negative which is set by the secondary mock event. If so, disregard the death so we don't log a death twice for a player.
            return;
        }
        dead.setLastDamage​(-1); //set the last damage to a negative value so we can differentiate between the real and mock death event sent from DMPMonitorCombat

        if (dead instanceof Player) {
            // Process Death
            SessionCache.getCachedSession(dead.getUniqueId()).ifPresent(Session::died);
        }

        try {
            EntityDamageEvent entityDamageEvent = dead.getLastDamageCause();
            if (!(entityDamageEvent instanceof EntityDamageByEntityEvent)) {
                return;
            }

            EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) entityDamageEvent;
            Entity killerEntity = entityDamageByEntityEvent.getDamager();

            UUID uuid = dead instanceof Player ? dead.getUniqueId() : null;
            handleKill(time, uuid, killerEntity);
        } catch (Exception e) {
            errorLogger.log(L.ERROR, e, ErrorContext.builder().related(event, dead).build());
        }
    }

    private void handleKill(long time, UUID victimUUID, Entity killerEntity) {
        Runnable processor = null;
        if (killerEntity instanceof Player) {
            processor = handlePlayerKill(time, victimUUID, (Player) killerEntity);
        } else if (killerEntity instanceof Tameable) {
            processor = handlePetKill(time, victimUUID, (Tameable) killerEntity);
        } else if (killerEntity instanceof Projectile) {
            processor = handleProjectileKill(time, victimUUID, (Projectile) killerEntity);
        }
        if (processor != null) {
            processing.submit(processor);
        }
    }

    private Runnable handlePlayerKill(long time, UUID victimUUID, Player killer) {
        Material itemInHand;
        try {
            itemInHand = killer.getInventory().getItemInMainHand().getType();
        } catch (NoSuchMethodError oldVersion) {
            try {
                itemInHand = killer.getInventory().getItemInHand().getType(); // Support for non dual wielding versions.
            } catch (Exception | NoSuchMethodError | NoSuchFieldError unknownError) {
                itemInHand = Material.AIR;
            }
        }
        String weaponName = new ItemNameFormatter().apply(itemInHand.name());
        
        return victimUUID != null
                ? new PlayerKillProcessor(killer.getUniqueId(), time, victimUUID, weaponName)
                : new MobKillProcessor(killer.getUniqueId());
    }

    private Runnable handlePetKill(long time, UUID victimUUID, Tameable tameable) {
        if (!tameable.isTamed()) {
            return null;
        }

        AnimalTamer owner = tameable.getOwner();
        if (!(owner instanceof Player)) {
            return null;
        }

        String name;
        try {
            name = tameable.getType().name();
        } catch (NoSuchMethodError oldVersionNoTypesError) {
            // getType introduced in 1.9
            name = tameable.getClass().getSimpleName();
        }

        return victimUUID != null
                ? new PlayerKillProcessor(owner.getUniqueId(), time, victimUUID, new EntityNameFormatter().apply(name))
                : new MobKillProcessor(owner.getUniqueId());
    }

    private Runnable handleProjectileKill(long time, UUID victimUUID, Projectile projectile) {
        ProjectileSource source = projectile.getShooter();
        if (!(source instanceof Player)) {
            return null;
        }

        Player player = (Player) source;
        String projectileName = new EntityNameFormatter().apply(projectile.getType().name());

        return victimUUID != null
                ? new PlayerKillProcessor(player.getUniqueId(), time, victimUUID, projectileName)
                : new MobKillProcessor(player.getUniqueId());
    }
}

