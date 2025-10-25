package com.luminol.luminolQoL.commands;

import com.luminol.luminolQoL.LuminolQoL;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SitCommand extends Command {

    private final LuminolQoL plugin;

    public SitCommand(LuminolQoL plugin, String name, String description, String usageMessage, List<String> aliases) {
        super(name, description, usageMessage, aliases);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission("sit.command")) return true;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Sitables config reloaded successfully!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /sit reload");
        }
        return true;
    }
}
