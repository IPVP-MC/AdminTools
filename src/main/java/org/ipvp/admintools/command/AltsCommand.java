package org.ipvp.admintools.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import org.ipvp.admintools.AdminTools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AltsCommand extends AdminToolsCommand {

    public AltsCommand(AdminTools plugin) {
        super(plugin, "alts", "admintools.command.alts");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /alts <player>");
            return;
        }

        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID uuid = getUuidFromArg(connection, 0, args);

            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                try (PreparedStatement pageEntries = connection.prepareStatement("SELECT DISTINCT name " +
                                "FROM player_login " +
                                "WHERE ip_address " +
                                "IN (SELECT DISTINCT ip_address " +
                                        "FROM player_login " +
                                        "WHERE id = ?)")) {

                    pageEntries.setString(1, uuid.toString());

                    try (ResultSet rs = pageEntries.executeQuery()) {
                        List<String> names = new ArrayList<>();
                        while (rs.next()) {
                            String name = rs.getString("name");
                            if (!name.equalsIgnoreCase(args[0])) {
                                names.add(rs.getString("name"));
                            }
                        }

                        if (names.isEmpty()) {
                            sender.sendMessage(ChatColor.RED + "Target player has no alternate accounts");
                            return;
                        }

                        sender.sendMessage(ChatColor.YELLOW + args[0] + " has shared an IP address with the following users:");
                        sender.sendMessage(names.stream().collect(Collectors.joining(", ")));
                    }
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when checking alts of " + args[0]);
            getPlugin().getLogger().log(Level.SEVERE, "Failed to check alts", e);
        }
    }
}
