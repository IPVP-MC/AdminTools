package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class Ban extends Punishment<UUID> {

    public Ban(int id, UUID sender, UUID banned, String reason, Timestamp created, Timestamp expiry) {
        super(id, sender, banned, reason, created, expiry);
    }
}
