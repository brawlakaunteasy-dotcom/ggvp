package com.exantibotfilter.command;

import com.exantibotfilter.ExAntiBotFilter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final ExAntiBotFilter plugin;

    public AdminCommand(ExAntiBotFilter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("exantibotfilter.admin")) {
            sender.sendMessage(plugin.prefix() + plugin.color(plugin.getConfig().getString("messages.no-permission", "&cSizda huquq yo'q")));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.prefix() + plugin.color("&7ExAntiBotFilter &av" + plugin.getDescription().getVersion()));
            sender.sendMessage(plugin.prefix() + plugin.color("&7Buyruqlar: &e/eabf reload"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(plugin.prefix() + plugin.color(plugin.getConfig().getString("messages.reload-success", "&aQayta yuklandi")));
            return true;
        }
        return false;
    }
}
