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
import java.util.logging.Level;

public class WarnCommand extends AdminToolsCommand {

    public WarnCommand(AdminTools plugin) {
        super(plugin, "warn", "admintools.command.warn");
        // "/mute <player> <reason>"
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/warn <player> <reason>");
            return;
        }

        // ban <player> [duration] <reason>
        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID id = getUuidFromArg(connection, 0, args);

            if (id == null) {
                // Never joined the server
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                String reason = getReasonFromArgs(1, args);

                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO player_punish(banned_id, sender_id, reason, creation_date, expiry_date, type) " +
                        "VALUES (?, ?, ?, ?, ?, 'warn')")) {
                    ps.setString(1, id.toString());
                    ps.setString(2, getPlugin().getUniqueIdSafe(sender));
                    ps.setString(3, reason);
                    ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    ps.setTimestamp(5, null);
                    ps.executeUpdate();
                }

                ProxiedPlayer target = getPlugin().getProxy().getPlayer(id);
                if (target != null) {
                    target.sendMessage(ChatColor.RED + "You were warned by " + sender.getName() + " for " + reason);
                }
                String name = target == null ? args[0] : target.getName();
                // Broadcast minimal for all players
                getPlugin().broadcast(ChatColor.RED + String.format("%s was warned by %s", name, sender.getName()), "admintools.notify.warn.minimal");
                // Broadcast full message
                getPlugin().broadcast(ChatColor.RED + String.format("%s was warned by %s for %s", name, sender.getName(), reason), "admintools.notify.warn.full");
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when issuing the warning");
            getPlugin().getLogger().log(Level.SEVERE, "Failed to issue warning", e);
        }
    }
}
