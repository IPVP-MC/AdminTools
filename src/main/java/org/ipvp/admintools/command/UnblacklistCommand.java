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

public class UnblacklistCommand extends AdminToolsCommand {

    public UnblacklistCommand(AdminTools plugin) {
        super(plugin, "unblacklist", "admintools.command.unblacklist");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender != getPlugin().getProxy().getConsole()) {
            sender.sendMessage(ChatColor.RED + "Blacklist bans may only be removed by the console");
            return;
        } else if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unblacklist <player>");
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
                if (ban == null || !ban.isBlacklist()) {
                    sender.sendMessage(ChatColor.RED + "That user is not blacklisted");
                } else {
                   try (PreparedStatement insertUnban = connection.prepareStatement("INSERT INTO player_punish_reverse" +
                            "(punish_id, banned_id) " +
                            "VALUES (?, ?)")) {
                        insertUnban.setInt(1, ban.getId());
                        insertUnban.setString(2, ban.getPunished().toString());
                        insertUnban.executeUpdate();
                    }

                    getPlugin().broadcast(ChatColor.RED + String.format("%s was unbanned by %s", args[0], sender.getName()), "admintools.notify.unban");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when issuing the unban");
            getPlugin().getLogger().log(Level.SEVERE, "Failed to issue unblacklist", e);
        }
    }
}
