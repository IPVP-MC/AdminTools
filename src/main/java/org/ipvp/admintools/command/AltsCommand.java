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

    private static final int ENTRIES_PER_PAGE = 25;

    public AltsCommand(AdminTools plugin) {
        super(plugin, "alts", "admintools.command.alts");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /alts <player> [page]");
            return;
        }

        int page;
        try {
            page = args.length == 1 ? 0 : Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Please enter a valid page number");
            sender.sendMessage(ChatColor.RED + "Usage: /alts <player> [page]");
            return;
        }

        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID uuid = getUuidFromArg(connection, 0, args);

            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                int maxPages = getMaxPages(connection, uuid);

                if (maxPages == 0) {
                    sender.sendMessage(ChatColor.RED + "Target player has no alternate accounts");
                    return;
                }

                if (page + 1 > maxPages || page < 0) {
                    sender.sendMessage(ChatColor.RED + "You must enter a page number between 1 and " + maxPages);
                    sender.sendMessage(ChatColor.RED + "Usage: /alts <player> [page]");
                    return;
                }

                try (PreparedStatement pageEntries = connection.prepareStatement("SELECT * FROM (" +
                        "SELECT * FROM player_related_ip_login ORDER BY time1" +
                        ") ordered " +
                        "WHERE id1 = ? " +
                        "GROUP BY id1, id2 " +
                        "LIMIT ?,?")) {

                    pageEntries.setString(1, uuid.toString());
                    pageEntries.setInt(2, page * ENTRIES_PER_PAGE);
                    pageEntries.setInt(3, (page * ENTRIES_PER_PAGE) + ENTRIES_PER_PAGE);

                    try (ResultSet rs = pageEntries.executeQuery()) {
                        sender.sendMessage(ChatColor.YELLOW + args[0] + " has shared an IP address with the following users:");
                        List<String> names = new ArrayList<>();
                        while (rs.next()) {
                            names.add(rs.getString("name2"));
                        }
                        sender.sendMessage(names.stream().collect(Collectors.joining(", ")));
                    }
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when checking alts of " + args[0]);
            getPlugin().getLogger().log(Level.SEVERE, "Failed to check alts", e);
        }
    }

    private int getMaxPages(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement maxPages = connection.prepareStatement("SELECT count(*) / ? max_pages " +
                "FROM (SELECT * FROM player_related_ip_login " +
                "WHERE id1 = ? " +
                "GROUP BY id1, id2) subquery")) {
            maxPages.setInt(1, ENTRIES_PER_PAGE);
            maxPages.setString(2, id.toString());
            try (ResultSet rs = maxPages.executeQuery()) {
                return rs.next() ? (int) Math.ceil(rs.getDouble("max_pages")) : -1;
            }
        }
    }
}
