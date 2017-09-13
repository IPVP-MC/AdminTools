package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class Ban extends Punishment {

    private final UUID banned;

    public Ban(int id, UUID sender, UUID banned, String reason, Timestamp created, Timestamp expiry) {
        super(id, sender, reason, created, expiry);
        this.banned = banned;
    }

    public UUID getBanned() {
        return banned;
    }
}
