package com.luminol.luminolQoL;

import com.luminol.luminolQoL.commands.AutoReplantCommand;
import com.luminol.luminolQoL.listeners.*;
import com.luminol.luminolQoL.managers.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LuminolQoL extends JavaPlugin {
    public static final String VERSION = "1.0";

    private final Set<UUID> disabledPlayers = new HashSet<>();
    private DynamicLightingListener dynamicLightingListener;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Save and reload config
        saveDefaultConfig();
        reloadConfig();

        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.load();

        // Register dynamic lighting
        dynamicLightingListener = new DynamicLightingListener(this);
        getServer().getPluginManager().registerEvents(dynamicLightingListener, this);

        // Register other features
        getServer().getPluginManager().registerEvents(new DeathMessageListener(this, configManager), this);
        getServer().getPluginManager().registerEvents(new TotemInventoryListener(), this);
        getServer().getPluginManager().registerEvents(new SitInteractListener(this, configManager), this);
        getServer().getPluginManager().registerEvents(new SitDismountListener(), this);
        getServer().getPluginManager().registerEvents(new CropHarvestListener(this, configManager), this);
        getServer().getPluginManager().registerEvents(new ItemStackListener(this), this);
        getServer().getPluginManager().registerEvents(new VeinMinerListener(this), this);

        // Register commands
        getCommand("autoreplant").setExecutor(new AutoReplantCommand(configManager));

        getServer().getConsoleSender().sendMessage("[LuminolQoL] §aPlugin loaded successfully with all features!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dynlight")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§c/dynlight on - Enable dynamic lighting");
                sender.sendMessage("§c/dynlight off - Disable dynamic lighting");
                sender.sendMessage("§c/dynlight reload - Reload config");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "on" -> {
                    disabledPlayers.remove(player.getUniqueId());
                    player.sendMessage("§aDynamic lighting enabled.");
                }
                case "off" -> {
                    disabledPlayers.add(player.getUniqueId());
                    player.sendMessage("§cDynamic lighting disabled.");
                }
                case "reload" -> {
                    if (!player.isOp() && !player.hasPermission("luminolqol.reload")) {
                        player.sendMessage("§cYou do not have permission to reload the plugin.");
                        return true;
                    }
                    reloadConfig();
                    configManager.reload();
                    player.sendMessage("§aLuminolQoL configuration reloaded!");
                }
                default -> {
                    player.sendMessage("§cUnknown subcommand.");
                    player.sendMessage("§c/dynlight on - Enable dynamic lighting");
                    player.sendMessage("§c/dynlight off - Disable dynamic lighting");
                    player.sendMessage("§c/dynlight reload - Reload config");
                }
            }
            return true;
        }
        return false;
    }

    public boolean isDynamicLightEnabled(Player player) {
        return !disabledPlayers.contains(player.getUniqueId());
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
