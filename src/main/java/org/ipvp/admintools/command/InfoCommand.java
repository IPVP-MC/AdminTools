package org.ipvp.admintools.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.ipvp.admintools.AdminTools;
import org.ipvp.admintools.util.TimeFormatUtil;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.logging.Level;

public class InfoCommand extends AdminToolsCommand {

    private static final int ENTRIES_PER_PAGE = 10;
    private final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");

    public InfoCommand(AdminTools plugin) {
        super(plugin, "info", "admintools.command.info");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /info <player> [page]");
            return;
        }

        int page;
        try {
            page = args.length == 1 ? 0 : Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Please enter a valid page number");
            sender.sendMessage(ChatColor.RED + "Usage: /info <player> [page]");
            return;
        }

        try (Connection connection = getPlugin().getDatabase().getConnection()) {
            UUID uuid = getUuidFromArg(connection, 0, args);

            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "That player has never joined the server");
            } else {
                int maxPages = getMaxPages(connection, uuid);

                if (page + 1 > maxPages || page < 0) {
                    sender.sendMessage(ChatColor.RED + "You must enter a page number between 1 and " + maxPages);
                    sender.sendMessage(ChatColor.RED + "Usage: /info <player> [page]");
                    return;
                }

                sender.sendMessage(ChatColor.GRAY + "Fetching punishment information...");
                try (PreparedStatement pageEntries = connection.prepareStatement("SELECT name, " +
                        "pr.sender_id reverse_sender_id, " +
                        "pr.reason reverse_reason, " +
                        "pr.creation_date reverse_date, " +
                        "p.reason, " +
                        "p.creation_date, " +
                        "expiry_date, " +
                        "type " +
                        "FROM player_punish p " +
                        "LEFT JOIN player_punish_reverse pr " +
                        "ON pr.punish_id = p.id " +
                        "LEFT JOIN player_latest_login l " +
                        "ON l.id = p.sender_id " +
                        "WHERE p.banned_id = ? " +
                        "ORDER BY creation_date DESC " +
                        "LIMIT ?, ?")) {

                    int amount = 0;
                    pageEntries.setString(1, uuid.toString());
                    pageEntries.setInt(2, page * ENTRIES_PER_PAGE);
                    pageEntries.setInt(3, (page * ENTRIES_PER_PAGE) + ENTRIES_PER_PAGE);

                    try (ResultSet rs = pageEntries.executeQuery()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                String.format("&ePunishments of %s &7(Page %d/%d)", args[0], page + 1, maxPages)));
                        while (rs.next()) {
                            String raw = rs.getString("name");
                            String name = raw == null ? "Console" : raw;
                            String reason = rs.getString("reason");
                            Timestamp created = rs.getTimestamp("creation_date");
                            Timestamp expiry = rs.getTimestamp("expiry_date");
                            String type = rs.getString("type");

                            ComponentBuilder builder = new ComponentBuilder(" ");
                            ComponentBuilder hoverBuilder = new ComponentBuilder("Reason: ").color(ChatColor.GRAY)
                                    .append(reason).color(ChatColor.WHITE);

                            if (rs.getObject("reverse_date") != null) {
                                builder.append("").strikethrough(true);
                                String reversed = rs.getString("reverse_sender_id");
                                String reverseName = reversed == null ? "Console" : getNameFromUuid(connection, UUID.fromString(reversed));

                                hoverBuilder.append("\n").append("Reversed By: ").color(ChatColor.GRAY)
                                        .append(reverseName).color(ChatColor.WHITE);

                                String reverseReason = rs.getString("reverse_reason");
                                if (reverseReason != null) {
                                    builder.append("\n").append("Reverse Reason: ").color(ChatColor.GRAY)
                                            .append(reverseReason).color(ChatColor.WHITE);
                                }
                            }

                            builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));

                            if (type.equalsIgnoreCase("warn")) {
                                builder.append("[Warn]").color(ChatColor.YELLOW).append(" ");
                            } else if (type.equalsIgnoreCase("mute")) {
                                builder.append("[Mute]").color(ChatColor.GOLD).append(" ").color(ChatColor.BOLD);
                            } else {
                                builder.append("[Ban]").color(ChatColor.RED).append(" ").append(" ").color(ChatColor.BOLD);
                            }

                            long duration = expiry == null ? -1 : expiry.getTime() - created.getTime();
                            builder.strikethrough(false).append(dateFormat.format(created)).color(ChatColor.GREEN)
                                    .append(" ").append(name).color(ChatColor.GRAY)
                                    .append(" (").color(ChatColor.RED)
                                    .append(duration == -1 ? "Permanent" : TimeFormatUtil.toDetailedDate(0, duration, true))
                                    .append(")");

                            if (expiry != null && expiry.getTime() < System.currentTimeMillis()) {
                                builder.append("(Expired)").color(ChatColor.RED);
                            }

                            sender.sendMessage(builder.create());

                            amount++;
                        }

                        if (amount == 0) {
                            sender.sendMessage(ChatColor.RED + " None!");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred when getting info of " + args[0]);
            getPlugin().getLogger().log(Level.SEVERE, "Failed to check info", e);
        }
    }

    private int getMaxPages(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement maxPages = connection.prepareStatement("SELECT count(*) / ? max_pages  " +
                "FROM player_punish " +
                "WHERE banned_id = ?")) {
            maxPages.setInt(1, ENTRIES_PER_PAGE);
            maxPages.setString(2, id.toString());
            try (ResultSet rs = maxPages.executeQuery()) {
                return rs.next() ? (int) Math.ceil(rs.getDouble("max_pages")) : -1;
            }
        }
    }
}
