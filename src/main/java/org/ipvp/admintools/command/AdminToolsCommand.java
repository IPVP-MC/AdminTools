package org.ipvp.admintools.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.ipvp.admintools.AdminTools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AdminToolsCommand extends Command {

    private AdminTools plugin;

    public AdminToolsCommand(AdminTools plugin, String name, String permission) {
        super(name, permission);
        this.plugin = plugin;
    }

    public AdminTools getPlugin() {
        return plugin;
    }

    @Override
    public final void execute(final CommandSender commandSender, final String[] args) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> onCommand(commandSender, args));
    }

    public abstract void onCommand(CommandSender sender, String[] args);


    protected String getReasonFromArgs(int index, String[] args) {
        return Arrays.stream(args, index, args.length).collect(Collectors.joining(" "));
    }

    protected UUID getUuidFromArg(Connection connection, int index, String[] args) throws SQLException {
        return getUuidFromArg(connection, args[index]);
    }

    protected UUID getUuidFromArg(Connection connection, String arg) throws SQLException {
        try {
            return UUID.fromString(arg);
        } catch (IllegalArgumentException e) {
            ProxiedPlayer online = getPlugin().getProxy().getPlayer(arg);
            if (online != null) {
                return online.getUniqueId();
            }
            try (PreparedStatement ps = connection.prepareStatement("SELECT id " +
                    "FROM player_login " +
                    "WHERE name = ? " +
                    "ORDER BY time DESC " +
                    "LIMIT 1")) {
                ps.setString(1, arg);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? UUID.fromString(rs.getString("id")) : null;
                }
            }
        }
    }

    protected String getNameFromUuid(Connection connection, UUID uuid) throws SQLException {
        ProxiedPlayer online = getPlugin().getProxy().getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT name " +
                "FROM player_login " +
                "WHERE id = ? " +
                "ORDER BY time DESC " +
                "LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        }
    }
}