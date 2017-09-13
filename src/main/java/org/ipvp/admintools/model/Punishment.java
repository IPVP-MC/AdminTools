package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class Punishment {

    private final int id;
    private final UUID sender;
    private final String reason;
    private final Timestamp created;
    private final Timestamp expiry;

    public Punishment(int id, UUID sender, String reason, Timestamp created, Timestamp expiry) {
        this.id = id;
        this.sender = sender;
        this.reason = reason;
        this.created = created;
        this.expiry = expiry;
    }

    public int getId() {
        return id;
    }

    public UUID getSender() {
        return sender;
    }

    public String getReason() {
        return reason;
    }

    public Timestamp getCreated() {
        return created;
    }

    public Timestamp getExpiry() {
        return expiry;
    }
}
