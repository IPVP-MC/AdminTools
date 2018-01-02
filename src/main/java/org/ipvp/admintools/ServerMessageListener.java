package org.ipvp.admintools;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.ipvp.admintools.command.BanCommand;
import org.ipvp.admintools.model.Ban;
import org.ipvp.admintools.model.IpBan;
import org.ipvp.admintools.model.Mute;
import org.ipvp.admintools.model.Punishment;
import org.ipvp.admintools.util.TimeFormatUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ServerMessageListener implements Listener {

    private AdminTools plugin;

    public ServerMessageListener(AdminTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMessageReceive(PluginMessageEvent event) {
        if (event.getTag().equals("BungeeCord")) {
            ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
            String sub = input.readUTF();
            if (!sub.equals("BanPlayer")) {
                return;
            }

            String name = input.readUTF();
            UUID uuid = UUID.fromString(input.readUTF());
            String reason = input.readUTF();

            try (Connection connection = plugin.getDatabase().getConnection()) {
                BanCommand.banPlayer(plugin, ProxyServer.getInstance().getConsole(), connection, name, uuid, -1, reason);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to issue ban", e);
            }
        } else if (event.getTag().equals("BanUser")) {
            ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
            UUID user = UUID.fromString(input.readUTF());
            ProxiedPlayer player = plugin.getProxy().getPlayer(user);

            if (player == null) {
                return;
            }

            String reason = input.readUTF();

            try (Connection connection = plugin.getDatabase().getConnection()) {
                BanCommand.banPlayer(plugin, ProxyServer.getInstance().getConsole(), connection, player.getName(), user, -1, reason);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to issue ban", e);
            }
        }
    }
}
