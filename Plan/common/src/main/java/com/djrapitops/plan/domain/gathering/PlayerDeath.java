package com.djrapitops.plan.domain.gathering;

import java.util.UUID;

public class PlayerDeath {
    public final UUID victimUUID;
    public final KillerEntity killedBy;

    public PlayerDeath(UUID victimUUID, KillerEntity killedBy) {
        this.victimUUID = victimUUID;
        this.killedBy = killedBy;
    }
}
