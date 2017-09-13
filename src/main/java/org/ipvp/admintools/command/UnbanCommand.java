package org.ipvp.admintools.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import org.ipvp.admintools.AdminTools;
import org.ipvp.admintools.model.Ban;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class UnbanCommand extends AdminToolsCommand {

    public UnbanCommand(AdminTools plugin, String name) {
        super(plugin, name, "admintools.command.unban");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            // TODO: Better usage messages
            sender.sendMessage(ChatColor.RED + "Usage: /unban <player> [reason]");
            return;
        }

        // ban <player> [duration] <reason>
        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID id = getUuidFromArg(connection, 0, args);

            if (id == null) {
                // Never joined the server
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                Ban ban = getPlugin().getActiveBan(connection, id);
                if (ban == null) {
                    sender.sendMessage(ChatColor.RED + "That user is not banned");
                } else {
                    String reason = args.length > 1 ? getReasonFromArgs(1, args) : null;

                    if (reason == null && !sender.hasPermission("admintools.command.unban.no-reason")) { // Check sender perms
                        sender.sendMessage(ChatColor.RED + "Please specify a valid unban reason");
                        sender.sendMessage(ChatColor.RED + "Usage: /unban <player> [reason]");
                        return;
                    }

                    try (PreparedStatement insertUnban = connection.prepareStatement("INSERT INTO player_punish_reverse" +
                            "(punish_id, banned_id, sender_id, reason) " +
                            "VALUES (?, ?, ?, ?)")) {
                        insertUnban.setInt(1, ban.getId());
                        insertUnban.setString(2, ban.getPunished().toString());
                        insertUnban.setString(2, getPlugin().getUniqueId(sender).toString());
                        insertUnban.setString(3, reason);
                        insertUnban.executeUpdate();
                    }

                    // TODO: broadcasts, etc
                    sender.sendMessage(ChatColor.GREEN + "You have unbanned " + args[0]);
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when issuing the ban");
            getPlugin().getLogger().log(Level.SEVERE, "Failed to issue ban", e);
        }
    }
}
