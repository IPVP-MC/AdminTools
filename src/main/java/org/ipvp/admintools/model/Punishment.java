package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class Punishment<T> {

    private final int id;
    private final UUID sender;
    private final T punished;
    private final String reason;
    private final Timestamp created;
    private final Timestamp expiry;

    public Punishment(int id, UUID sender, T punished, String reason, Timestamp created, Timestamp expiry) {
        this.id = id;
        this.sender = sender;
        this.punished = punished;
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

    public T getPunished() {
        return punished;
    }
}
