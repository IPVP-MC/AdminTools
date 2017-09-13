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
import java.sql.Timestamp;
import java.util.UUID;
import java.util.logging.Level;

public class MuteCommand extends AdminToolsCommand {

    public MuteCommand(AdminTools plugin, String name) {
        super(plugin, name, "admintools.command.mute");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        // mute <player> [duration] <reason>
        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID id = getUuidFromArg(connection, 0, args);

            if (id == null) {
                // Never joined the server
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                Mute mute = getPlugin().getActiveMute(connection, id);
                if (mute != null) {
                    sender.sendMessage(ChatColor.RED + "That user is already muted");
                } else {
                    String reason;
                    long expiryDate;
                    long duration = TimeFormatUtil.parseIntoMilliseconds(args[1]);
                    if (duration == -1) {
                        expiryDate = Long.MAX_VALUE;
                        reason = getReasonFromArgs(1, args);
                    } else {
                        expiryDate = System.currentTimeMillis() + duration;
                        reason = getReasonFromArgs(2, args);
                    }

                    try (PreparedStatement insertBan =
                                 connection.prepareStatement("INSERT INTO player_punish(banned_id, sender_id, reason, expiry_date, type) " +
                                         "VALUES (?, ?, ?, ?, ?)")) {
                        insertBan.setString(1, id.toString());
                        insertBan.setString(2, sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId().toString() : null);
                        insertBan.setString(3, reason);
                        insertBan.setTimestamp(4, new Timestamp(expiryDate));
                        insertBan.setString(5, "mute");
                        insertBan.executeUpdate();
                    }

                    // TODO: broadcasts, etc
                    sender.sendMessage(ChatColor.GREEN + "You have muted " + args[0]);
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when issuing the ban");
            getPlugin().getLogger().log(Level.SEVERE, "Failed to issue ban", e);
        }
    }
}
