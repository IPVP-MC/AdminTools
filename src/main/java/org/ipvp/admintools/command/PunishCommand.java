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

public abstract class PunishCommand extends AdminToolsCommand {

    private String usage;

    protected PunishCommand(AdminTools plugin, String name, String permission, String usage) {
        super(plugin, name, permission);
        this.usage = ChatColor.RED + "Usage: " + usage;
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(usage);
            return;
        }

        // ban <player> [duration] <reason>
        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID id = getUuidFromArg(connection, 0, args);

            if (id == null) {
                // Never joined the server
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                Punishment<?> punishment = getExistingPunishment(connection, id);
                if (punishment != null) {
                    sender.sendMessage(ChatColor.RED + "That user has already been dealt with");
                } else {
                    String reason;
                    long expiryDate;
                    long duration = TimeFormatUtil.parseIntoMilliseconds(args[1]);
                    if (duration == -1) {
                        if (!sender.hasPermission("admintools.command.punish.permanent")) {
                            sender.sendMessage(ChatColor.RED + "Please specify a valid duration");
                            sender.sendMessage(usage);
                            return;
                        }
                        expiryDate = -1;
                        reason = getReasonFromArgs(1, args);
                    } else if (args.length < 3) {
                        sender.sendMessage(usage);
                        return;
                    } else {
                        expiryDate = System.currentTimeMillis() + duration;
                        reason = getReasonFromArgs(2, args);
                    }

                    issueNewPunishment(sender, connection, args[0], id, expiryDate, reason);
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when issuing the ban");
            getPlugin().getLogger().log(Level.SEVERE, "Failed to issue ban", e);
        }
    }

    public abstract Punishment<?> getExistingPunishment(Connection connection, UUID id);
    
    public abstract void issueNewPunishment(CommandSender sender, Connection connection, String targetName, UUID id, long expiry, String reason);
}
