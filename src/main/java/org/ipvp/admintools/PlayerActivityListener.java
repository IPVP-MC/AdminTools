package org.ipvp.admintools;

import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.ipvp.admintools.model.Ban;

import java.sql.Connection;
import java.util.UUID;

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
                // TODO: Check IP ban
            }
        } catch (Exception e) {
            event.setCancelled(true);
            event.setCancelReason("An error occurred when checking user ban information");
        } finally {
            event.completeIntent(plugin);
        }
    }
}
