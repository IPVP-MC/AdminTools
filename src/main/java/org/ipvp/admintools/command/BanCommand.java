package org.ipvp.admintools.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.ipvp.admintools.AdminTools;
import org.ipvp.admintools.model.Punishment;
import org.ipvp.admintools.util.TimeFormatUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class BanCommand extends PunishCommand {

    public BanCommand(AdminTools plugin) {
        super(plugin, "ban", "admintools.command.ban",
                "/ban <player> [duration] <reason>");
    }

    @Override
    public Punishment<?> getExistingPunishment(Connection connection, UUID id) {
        try {
            return getPlugin().getActiveBan(connection, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void issueNewPunishment(CommandSender sender, Connection connection, String targetName, UUID id, long expiry, String reason) {
        try (PreparedStatement insertBan =
                     connection.prepareStatement("INSERT INTO player_punish(banned_id, sender_id, reason, expiry_date, type) " +
                             "VALUES (?, ?, ?, ?, 'ban')")) {
            insertBan.setString(1, id.toString());
            insertBan.setString(2, getPlugin().getUniqueId(sender).toString());
            insertBan.setString(3, reason);
            insertBan.setTimestamp(4, new Timestamp(expiry));
            insertBan.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ProxiedPlayer target = getPlugin().getProxy().getPlayer(id);
        String timeFormatted = TimeFormatUtil.toDetailedDate(expiry, true);
        if (target != null) {
            target.disconnect(String.format("You were banned by %s for %s [%s]", sender.getName(), reason, timeFormatted));
        }
        String name = target == null ? targetName : target.getName();
        // Broadcast minimal for all players
        getPlugin().broadcast(ChatColor.RED + String.format("%s was banned by %s", name, sender.getName()), "admintools.notify.ban.minimal");
        // Broadcast full message
        getPlugin().broadcast(ChatColor.RED + String.format("%s was banned by %s for %s [%s]", name, sender.getName(), reason, timeFormatted), "admintools.notify.ban.full");
    }
}
