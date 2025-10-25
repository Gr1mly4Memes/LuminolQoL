package com.luminol.luminolQoL.commands;

import com.luminol.luminolQoL.LuminolQoL;
import com.luminol.luminolQoL.managers.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager configManager;

    public ReloadCommand(LuminolQoL luminolQoL, ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("luminol.admin")) {
            sender.sendMessage(configManager.getPermissionMessage());
            return true;
        }

        configManager.reload();
        sender.sendMessage(configManager.getReloadMessage());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
