package org.ipvp.admintools.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.ipvp.admintools.AdminTools;

public abstract class AdminToolsCommand extends Command {

    private AdminTools plugin;

    public AdminToolsCommand(AdminTools plugin, String name) {
        super(name);
        this.plugin = plugin;
    }

    @Override
    public final void execute(final CommandSender commandSender, final String[] args) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> onCommand(commandSender, args));
    }

    public abstract void onCommand(CommandSender sender, String[] args);
}