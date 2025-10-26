package com.luminol.luminolQoL.listeners;

import com.luminol.luminolQoL.LuminolQoL;
import com.luminol.luminolQoL.managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class DeathMessageListener implements Listener {

    private final LuminolQoL plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    public DeathMessageListener(LuminolQoL plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Check if feature is enabled
        if (!plugin.getConfig().getBoolean("funny-death-messages.enabled", true)) {
            return;
        }

        Player player = event.getEntity();
        if (player == null) return;

        DamageCause cause = (player.getLastDamageCause() != null)
                ? player.getLastDamageCause().getCause()
                : DamageCause.CUSTOM;

        String causeKey = cause.name().toLowerCase();

        // Get messages from config
        Map<String, Object> section = plugin.getConfig()
                .getConfigurationSection("funny-death-messages")
                .getValues(false);

        List<String> messages = plugin.getConfig().getStringList("funny-death-messages." + causeKey);

        // Fallback to default if empty
        if ((messages == null || messages.isEmpty()) && section.containsKey("default")) {
            messages = plugin.getConfig().getStringList("funny-death-messages.default");
        }

        if (messages == null || messages.isEmpty()) return;

        String message = messages.get(random.nextInt(messages.size()));

        // Determine killer
        String killerName = "unknown";
        Entity killerEntity = null;

        if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent ede) {
            killerEntity = ede.getDamager();
        }

        if (killerEntity != null) {
            if (killerEntity instanceof Player p) {
                killerName = p.getName();
            } else if (killerEntity.getCustomName() != null) {
                killerName = killerEntity.getCustomName();
            } else {
                killerName = formatEntityName(killerEntity.getType().name());
            }
        }

        // Replace placeholders
        message = message
                .replace("{player}", player.getName())
                .replace("{killer}", killerName)
                .replace("{world}", player.getWorld().getName())
                .replace("{cause}", cause.name().replace("_", " ").toLowerCase());

        event.setDeathMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private String formatEntityName(String name) {
        // Converts ZOMBIE_PIGMAN to Zombie Pigman
        name = name.toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }
}
