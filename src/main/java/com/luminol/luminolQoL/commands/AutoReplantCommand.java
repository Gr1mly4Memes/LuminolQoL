package com.luminol.luminolQoL.commands;

import com.luminol.luminolQoL.LuminolQoL;
import com.luminol.luminolQoL.managers.ConfigManager;
import com.luminol.luminolQoL.utility.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("NullableProblems")
public class AutoReplantCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager configManager;

    public AutoReplantCommand(final ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String alias, final String[] args) {
        if (args.length == 1) {
            final String subCommand = args[0].toLowerCase();

            if (subCommand.equals("reload")) {
                if (!sender.hasPermission("autoreplant.admin")) {
                    sender.sendMessage(MessageUtil.format(this.configManager.getPermissionMessage()));
                    return true;
                }

                // Reload the config using your updated ConfigManager
                this.configManager.reload();
                sender.sendMessage(MessageUtil.format(this.configManager.getReloadMessage()));
                return true;
            }
        } else {
            sender.sendMessage(MessageUtil.format(
                    String.format("&d&lAutoReplant &7(%s)", LuminolQoL.VERSION)
            ));

            sender.sendMessage(MessageUtil.format(
                    "&8❘ &7Automatically replants harvested crops so you can harvest without ever planting again."
            ));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 1) {
            return Stream.of("reload")
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
