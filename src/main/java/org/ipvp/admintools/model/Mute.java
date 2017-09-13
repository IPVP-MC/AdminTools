package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class Mute extends Punishment<UUID> {

    public Mute(int id, UUID sender, UUID muted, String reason, Timestamp created, Timestamp expiry) {
        super(id, sender, muted, reason, created, expiry);
    }
}