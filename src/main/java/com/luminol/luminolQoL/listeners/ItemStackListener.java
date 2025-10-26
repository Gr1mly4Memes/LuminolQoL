package com.luminol.luminolQoL.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public class ItemStackListener implements Listener {

    private final JavaPlugin plugin;
    private final double mergeRadius;
    private final int checkDelay;

    public ItemStackListener(JavaPlugin plugin) {
        this.plugin = plugin;
        // Get config values with defaults
        this.mergeRadius = plugin.getConfig().getDouble("item-stacking.merge-radius", 2.0);
        this.checkDelay = plugin.getConfig().getInt("item-stacking.check-delay-ticks", 20);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        // Check if item stacking is enabled
        if (!plugin.getConfig().getBoolean("item-stacking.enabled", true)) {
            return;
        }

        Item spawnedItem = event.getEntity();

        // Schedule the merge check after a delay to reduce immediate server load
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Check if item is still valid
            if (spawnedItem.isDead() || !spawnedItem.isValid()) {
                return;
            }

            mergeNearbyItems(spawnedItem);
        }, checkDelay);
    }

    private void mergeNearbyItems(Item item) {
        if (item.isDead() || !item.isValid()) {
            return;
        }

        ItemStack itemStack = item.getItemStack();
        Location itemLocation = item.getLocation();

        // Get nearby entities within merge radius
        Collection<Item> nearbyItems = item.getNearbyEntities(mergeRadius, mergeRadius, mergeRadius)
                .stream()
                .filter(entity -> entity instanceof Item)
                .map(entity -> (Item) entity)
                .toList();

        for (Item nearbyItem : nearbyItems) {
            // Skip if it's the same item
            if (nearbyItem.equals(item)) {
                continue;
            }

            // Skip if nearby item is dead or invalid
            if (nearbyItem.isDead() || !nearbyItem.isValid()) {
                continue;
            }

            // Skip if items can't be picked up yet (just spawned)
            if (item.getPickupDelay() > 0 || nearbyItem.getPickupDelay() > 0) {
                continue;
            }

            ItemStack nearbyItemStack = nearbyItem.getItemStack();

            // Check if items are similar (same type, same meta)
            if (!itemStack.isSimilar(nearbyItemStack)) {
                continue;
            }

            // Calculate total amount
            int totalAmount = itemStack.getAmount() + nearbyItemStack.getAmount();
            int maxStackSize = itemStack.getMaxStackSize();

            // If combined amount fits in one stack
            if (totalAmount <= maxStackSize) {
                // Merge into the first item
                itemStack.setAmount(totalAmount);
                item.setItemStack(itemStack);

                // Remove the nearby item
                nearbyItem.remove();
            } else {
                // Fill the first item to max stack size
                int remaining = totalAmount - maxStackSize;
                itemStack.setAmount(maxStackSize);
                item.setItemStack(itemStack);

                // Update nearby item with remaining amount
                nearbyItemStack.setAmount(remaining);
                nearbyItem.setItemStack(nearbyItemStack);
            }
        }
    }
}

