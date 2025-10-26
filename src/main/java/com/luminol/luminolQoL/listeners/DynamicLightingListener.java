package com.luminol.luminolQoL.listeners;

import com.luminol.luminolQoL.LuminolQoL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles dynamic lighting for players and items.
 * Creates light blocks when players hold light-emitting items or when such items are dropped.
 */
public class DynamicLightingListener implements Listener {
    
    private final LuminolQoL plugin;
    private final Map<Player, Location> playerLightLocations = new HashMap<>();
    private final Map<Item, Location> itemLightLocations = new HashMap<>();
    private static final long UPDATE_INTERVAL_TICKS = 5L;

    public DynamicLightingListener(LuminolQoL plugin) {
        this.plugin = plugin;
        startLightUpdateTask();
    }

    /**
     * Handles player movement to update dynamic lighting.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        // Only update if player moved to a different block
        if (to == null || (to.getBlockX() == from.getBlockX() && 
                          to.getBlockY() == from.getBlockY() && 
                          to.getBlockZ() == from.getBlockZ())) {
            return;
        }

        if (!plugin.isDynamicLightEnabled(player)) {
            removePlayerLight(player);
            return;
        }

        updatePlayerLightFromInventory(player, to);
    }

    /**
     * Handles when player switches held item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        if (!plugin.isDynamicLightEnabled(player)) {
            removePlayerLight(player);
            return;
        }

        // Schedule for next tick to ensure the item switch has completed
        Bukkit.getScheduler().runTask(plugin, () -> {
            updatePlayerLightFromInventory(player, player.getLocation());
        });
    }

    /**
     * Cleans up player light when they quit.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerLight(event.getPlayer());
    }

    /**
     * Handles when player drops a light-emitting item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!plugin.isDynamicLightEnabled(player)) {
            return;
        }

        Item droppedItem = event.getItemDrop();
        int lightLevel = getLightLevel(droppedItem.getItemStack());

        if (lightLevel > 0) {
            // Remove player's light and add light to dropped item
            removePlayerLight(player);
            updateItemLight(droppedItem, lightLevel);
        }
    }

    /**
     * Handles when a light-emitting item spawns in the world.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        int lightLevel = getLightLevel(item.getItemStack());

        if (lightLevel > 0) {
            updateItemLight(item, lightLevel);
        }
    }

    /**
     * Handles when an entity picks up a light-emitting item.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        Item item = event.getItem();

        // Always try to remove item light to ensure cleanup
        removeItemLight(item);

        // If a player picked it up, update their light
        if (event.getEntity() instanceof Player player) {
            if (!plugin.isDynamicLightEnabled(player)) {
                return;
            }

            int lightLevel = getLightLevel(item.getItemStack());

            if (lightLevel > 0) {
                // Schedule for next tick to ensure item is in inventory
                Bukkit.getScheduler().runTask(plugin, () -> {
                    updatePlayerLightFromInventory(player, player.getLocation());
                });
            }
        }
    }

    /**
     * Handles when an item despawns to clean up its light.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        removeItemLight(item);
    }

    /**
     * Gets the light level for an item from config.
     */
    private int getLightLevel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }
        String itemType = item.getType().toString();
        return plugin.getConfig().getInt("light-sources." + itemType, 0);
    }

    /**
     * Updates player's light based on their current inventory.
     */
    private void updatePlayerLightFromInventory(Player player, Location location) {
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHandItem = inventory.getItemInMainHand();
        ItemStack offHandItem = inventory.getItemInOffHand();

        int lightLevel = Math.max(getLightLevel(mainHandItem), getLightLevel(offHandItem));

        if (lightLevel > 0) {
            updatePlayerLight(player, location, lightLevel);
        } else {
            removePlayerLight(player);
        }
    }

    /**
     * Updates or creates a light block for a player.
     */
    private void updatePlayerLight(Player player, Location location, int lightLevel) {
        Location currentLightLocation = playerLightLocations.get(player);

        // Remove old light block if it exists
        if (currentLightLocation != null && !currentLightLocation.equals(location)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Block oldBlock = currentLightLocation.getBlock();
                if (oldBlock.getType() == Material.LIGHT) {
                    oldBlock.setType(Material.AIR);
                }
            });
        }

        // Place new light block
        Bukkit.getScheduler().runTask(plugin, () -> {
            Block block = location.getBlock();
            if (block.getType() == Material.AIR || block.getType() == Material.LIGHT) {
                block.setType(Material.LIGHT);

                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData);

                playerLightLocations.put(player, location.clone());
            }
        });
    }

    /**
     * Removes a player's light block.
     */
    private void removePlayerLight(Player player) {
        Location lightLocation = playerLightLocations.remove(player);
        if (lightLocation != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Block block = lightLocation.getBlock();
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR);
                }
            });
        }
    }

    /**
     * Updates or creates a light block for a dropped item.
     */
    private void updateItemLight(Item item, int lightLevel) {
        Location itemLocation = item.getLocation();
        Location currentLightLocation = itemLightLocations.get(item);

        // Remove old light block if it exists
        if (currentLightLocation != null && !currentLightLocation.equals(itemLocation)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Block oldBlock = currentLightLocation.getBlock();
                if (oldBlock.getType() == Material.LIGHT) {
                    oldBlock.setType(Material.AIR);
                }
            });
        }

        // Place new light block
        Bukkit.getScheduler().runTask(plugin, () -> {
            Block block = itemLocation.getBlock();
            if (block.getType() == Material.AIR || block.getType() == Material.LIGHT) {
                block.setType(Material.LIGHT);

                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData);

                itemLightLocations.put(item, itemLocation.clone());
            }
        });
    }

    /**
     * Removes an item's light block.
     */
    private void removeItemLight(Item item) {
        Location lightLocation = itemLightLocations.remove(item);
        if (lightLocation != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Block block = lightLocation.getBlock();
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR);
                }
            });
        }
    }

    /**
     * Starts a repeating task to update lights for moving players and items.
     */
    private void startLightUpdateTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Update player lights
            playerLightLocations.forEach((player, currentLightLocation) -> {
                if (!player.isOnline() || !plugin.isDynamicLightEnabled(player)) {
                    return;
                }

                Location playerLocation = player.getLocation();
                if (!isSameBlock(playerLocation, currentLightLocation)) {
                    updatePlayerLightFromInventory(player, playerLocation);
                }
            });

            // Clean up dead/invalid items and update valid ones
            itemLightLocations.entrySet().removeIf(entry -> {
                Item item = entry.getKey();

                // Remove if item is dead or invalid
                if (item.isDead() || !item.isValid()) {
                    Location lightLocation = entry.getValue();
                    if (lightLocation != null && lightLocation.getBlock().getType() == Material.LIGHT) {
                        lightLocation.getBlock().setType(Material.AIR);
                    }
                    return true;
                }

                // Update light if item moved to a different block
                Location currentLightLocation = entry.getValue();
                Location itemLocation = item.getLocation();

                if (!isSameBlock(itemLocation, currentLightLocation)) {
                    updateItemLight(item, getLightLevel(item.getItemStack()));
                }

                return false;
            });
        }, 0L, UPDATE_INTERVAL_TICKS);
    }

    /**
     * Checks if two locations are in the same block.
     */
    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ() &&
               loc1.getWorld().equals(loc2.getWorld());
    }
}

