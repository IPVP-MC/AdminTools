package org.ipvp.admintools.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.ipvp.admintools.AdminTools;
import org.ipvp.admintools.model.Ban;
import org.ipvp.admintools.model.Mute;
import org.ipvp.admintools.util.TimeFormatUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class UnmuteCommand extends AdminToolsCommand {

    public UnmuteCommand(AdminTools plugin) {
        super(plugin, "unmute", "admintools.command.unmute");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            // TODO: Better usage messages
            sender.sendMessage(ChatColor.RED + "Usage: /unmute <player> [reason]");
            return;
        }

        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID id = getUuidFromArg(connection, 0, args);

            if (id == null) {
                // Never joined the server
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                Mute mute = getPlugin().getActiveMute(connection, id);
                if (mute == null) {
                    sender.sendMessage(ChatColor.RED + "That user is not muted");
                } else {
                    String reason = args.length > 1 ? getReasonFromArgs(1, args) : null;

                    if (reason == null && !sender.hasPermission("admintools.command.unmute.no-reason")) { // Check sender perms
                        sender.sendMessage(ChatColor.RED + "Please specify a valid unmute reason");
                        sender.sendMessage(ChatColor.RED + "Usage: /unmute <player> [reason]");
                        return;
                    }

                    try (PreparedStatement insertUnban = connection.prepareStatement("INSERT INTO player_punish_reverse" +
                            "(punish_id, banned_id, sender_id, reason) " +
                            "VALUES (?, ?, ?, ?)")) {
                        insertUnban.setInt(1, mute.getId());
                        insertUnban.setString(2, mute.getPunished().toString());
                        insertUnban.setString(3, getPlugin().getUniqueIdSafe(sender));
                        insertUnban.setString(4, reason);
                        insertUnban.executeUpdate();
                    }

                    ProxiedPlayer target = getPlugin().getProxy().getPlayer(id);
                    String name = target == null ? args[0] : target.getName();
                    getPlugin().broadcast(ChatColor.RED + String.format("%s was unmuted by %s", name, sender.getName()), "admintools.notify.unmute");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when issuing the unmute");
            getPlugin().getLogger().log(Level.SEVERE, "Failed to issue unmute", e);
        }
    }
}
