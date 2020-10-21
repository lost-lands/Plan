package com.djrapitops.plan.domain.delivery;

import java.util.UUID;

public class ServerUUID {
    public final UUID uuid;

    protected ServerUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public static ServerUUID from(UUID uuid) {
        return new ServerUUID(uuid);
    }

    public static ServerUUID fromString(String name) {
        return new ServerUUID(UUID.fromString(name));
    }
}
