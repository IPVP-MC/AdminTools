package org.ipvp.admintools.model;

import java.sql.Timestamp;
import java.util.UUID;

public class IpBan extends Punishment<String> {

    public IpBan(int id, UUID sender, String bannedIp, String reason, Timestamp created, Timestamp expiry) {
        super(id, sender, bannedIp, reason, created, expiry);
    }
}