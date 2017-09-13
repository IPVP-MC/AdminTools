package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class Mute extends Punishment {

    private final UUID banned;

    public Mute(int id, UUID sender, UUID banned, String reason, Timestamp created, Timestamp expiry) {
        super(id, sender, reason, created, expiry);
        this.banned = banned;
    }

    public UUID getBanned() {
        return banned;
    }
}