package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class IpBan extends Punishment {

    private final String bannedIp;

    public IpBan(int id, UUID sender, String bannedIp, String reason, Timestamp created, Timestamp expiry) {
        super(id, sender, reason, created, expiry);
        this.bannedIp = bannedIp;
    }

    public String getBannedIp() {
        return bannedIp;
    }
}