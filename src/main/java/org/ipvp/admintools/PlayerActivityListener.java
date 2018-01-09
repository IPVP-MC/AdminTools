package org.ipvp.admintools;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.ipvp.admintools.model.Ban;
import org.ipvp.admintools.model.IpBan;
import org.ipvp.admintools.model.Mute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PlayerActivityListener implements Listener {

    private AdminTools plugin;

    public PlayerActivityListener(AdminTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(LoginEvent event) {
        event.registerIntent(plugin);
        UUID who = event.getConnection().getUniqueId();
        try (Connection connection = plugin.getDatabase().getConnection()) {
            Ban ban = plugin.getActiveBan(connection, who);
            if (ban != null) {
                event.setCancelled(true);
                event.setCancelReason("You are currently banned from the network!"); // TODO: Ban information
            } else {
                String ip = event.getConnection().getAddress().getAddress().getHostAddress();
                IpBan ipBan = plugin.getActiveIpBan(connection, ip);
                if (ipBan != null) {
                    event.setCancelled(true);
                    event.setCancelReason("You are currently banned from the network!");
                }
            }
        } catch (Exception e) {
            event.setCancelled(true);
            event.setCancelReason("An error occurred when checking your ban information");
            plugin.getLogger().log(Level.SEVERE, "Failed to check user ban inforamtion", e);
        } finally {
            event.completeIntent(plugin);
        }
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        if (event.isCancelled()) {
            return;
        }

        event.registerIntent(plugin);
        try (Connection connection = plugin.getDatabase().getConnection();
             PreparedStatement insertLogin = connection.prepareStatement("INSERT INTO player_login(id, name, ip_address) " +
                     "VALUES (?, ?, INET_ATON(?))")) {
            PendingConnection pendingConnection = event.getConnection();
            insertLogin.setString(1, pendingConnection.getUniqueId().toString());
            insertLogin.setString(2, pendingConnection.getName());
            insertLogin.setString(3, pendingConnection.getAddress().getAddress().getHostAddress());
            insertLogin.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to log player login", e);
        } finally {
            event.completeIntent(plugin);
        }
    }

    @EventHandler
    public void onLoginNotifyAlts(LoginEvent event) {
        if (event.isCancelled()) {
            return;
        }

        event.registerIntent(plugin);
        try (Connection connection = plugin.getDatabase().getConnection();
             PreparedStatement findBannedAlts = connection.prepareStatement("SELECT name2 " +
                     "FROM player_related_ip_login " +
                     "JOIN player_active_punishment ON id2 = banned_id " +
                     "WHERE id1 = ? AND type = 'ban'")) {
            findBannedAlts.setString(1, event.getConnection().getUniqueId().toString());
            try (ResultSet alts = findBannedAlts.executeQuery()) {
                Set<String> bannedAlts = new TreeSet<>();
                while (alts.next()) {
                    bannedAlts.add(alts.getString("name2"));
                }

                if (bannedAlts.isEmpty()) {
                    return;
                }

                plugin.broadcast(ChatColor.RED + String.format("%s might be an alternate account of the following banned players: %s",
                        event.getConnection().getName(), bannedAlts.stream().collect(Collectors.joining(", "))), "admintools.notify.alts");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to notify player alt login", e);
        } finally {
            event.completeIntent(plugin);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        Mute mute;
        try {
            // TODO: Can't run blocking code
            mute = plugin.getActiveMute(player.getUniqueId());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check mute information of " + player.getName(), e);
            return;
        }

        // Check if the player is muted
        if (mute == null) {
            return;
        }
        // TODO: Better notification messages

        if (event.isCommand()) {
            if (isMutedCommand(event.getMessage())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot use this command while muted");
            }
            return;
        }

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You are currently muted");
    }

    private boolean isMutedCommand(String fullCommand) {
        String[] split = fullCommand.split(" ");
        for (int i = split.length ; i >= 0 ; i--) {
            String command = Arrays.stream(split, 0, i).collect(Collectors.joining(" "));
            if (plugin.getConfig().getStringList("mute-commands").contains(command.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
