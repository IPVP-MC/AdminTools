package org.ipvp.admintools;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.ipvp.admintools.model.Ban;
import org.ipvp.admintools.model.IpBan;
import org.ipvp.admintools.model.Mute;
import org.ipvp.admintools.util.TimeFormatUtil;

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
                String expiry = ban.getExpiry() == null ? "Permanent" : TimeFormatUtil.toDetailedDate(ban.getExpiry().getTime());
                event.setCancelReason(ChatColor.translateAlternateColorCodes('&',
                        String.format("&cYou are banned from &lIPVP" +
                                "\n\n&eReason: &f%s" +
                                "\nExpires: &f%s" +
                                "\n\n&eAppeal at http://ipvp.org/forum", ban.getReason(), expiry)));
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
            Mute mute = plugin.getActiveMute(connection, event.getConnection().getUniqueId());
            if (mute != null) {
                plugin.registerMute(mute.getPunished(), mute);
            }

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
             PreparedStatement findBannedAlts = connection.prepareStatement("SELECT DISTINCT name " +
                             "FROM player_login " +
                             "INNER JOIN (SELECT DISTINCT banned_id " +
                                "FROM player_active_punishment " +
                                "WHERE type = 'ban') AS b " +
                             "WHERE player_login.id = b.banned_id " +
                             "AND ip_address " +
                             "IN (SELECT DISTINCT ip_address " +
                                "FROM player_login " +
                                "WHERE id = ?)")) {
            findBannedAlts.setString(1, event.getConnection().getUniqueId().toString());
            try (ResultSet alts = findBannedAlts.executeQuery()) {
                Set<String> bannedAlts = new TreeSet<>();
                while (alts.next()) {
                    bannedAlts.add(alts.getString("name"));
                }

                if (bannedAlts.isEmpty()) {
                    return;
                }

                String formatted = bannedAlts.stream().limit(5).collect(Collectors.joining("\n&c"));

                if (bannedAlts.size() > 5) {
                    event.setCancelled(true);
                    event.setCancelReason(ChatColor.translateAlternateColorCodes('&',
                            String.format("&cYou are banned on too many accounts!" +
                                    "\n&c%s" +
                                    "\n&c..." +
                                    "\n&eAppeal at http://ipvp.org/forum", formatted)));
                    return;
                }

                plugin.broadcast(ChatColor.RED + String.format("%s is an alternate account of the following banned players: %s",
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
        Mute mute = plugin.getActiveMute(player);

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
        long expiry = mute.getExpiry() == null ? -1 : mute.getExpiry().getTime();
        if (expiry == -1) {
            player.sendMessage(ChatColor.RED + "You are permanently muted");
        } else {
            player.sendMessage(ChatColor.RED + "Your mute expires in " + TimeFormatUtil.toDetailedDate(expiry));
        }
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

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        plugin.unregisterMute(event.getPlayer());
    }
}
