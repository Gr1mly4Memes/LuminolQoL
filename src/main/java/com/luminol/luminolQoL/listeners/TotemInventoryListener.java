package com.luminol.luminolQoL.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TotemInventoryListener implements Listener {

    private static class SwapData {
        ItemStack originalOffhand;
        int originalTotemSlot;

        SwapData(ItemStack originalOffhand, int originalTotemSlot) {
            this.originalOffhand = originalOffhand;
            this.originalTotemSlot = originalTotemSlot;
        }
    }

    private final Map<UUID, SwapData> swappedItems = new HashMap<>();

    public TotemInventoryListener() {
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if this damage would kill the player
        if (player.getHealth() - event.getFinalDamage() > 0) return;

        PlayerInventory inv = player.getInventory();

        // If player already has totem in hand/offhand, let vanilla handle it
        if (inv.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) return;
        if (inv.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;

        // Search for totem in inventory
        int totemSlot = -1;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        // If we found a totem, swap it to offhand temporarily
        if (totemSlot != -1) {
            ItemStack totem = inv.getItem(totemSlot);
            ItemStack offhandItem = inv.getItemInOffHand();

            // Remember what was in offhand and where the totem was
            swappedItems.put(player.getUniqueId(), new SwapData(offhandItem, totemSlot));

            // Move totem to offhand
            inv.setItemInOffHand(totem);

            // Clear the original totem slot (we'll restore the offhand item there later)
            inv.setItem(totemSlot, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();

        // If we swapped items for this player, restore them after a tick
        if (swappedItems.containsKey(playerId)) {
            SwapData swapData = swappedItems.remove(playerId);

            // Schedule restoration for next tick to ensure totem effect has triggered
            Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getProvidingPlugin(getClass()),
                () -> {
                    PlayerInventory inv = player.getInventory();

                    // The totem should have been consumed by now (offhand should be empty)
                    // Restore the original offhand item
                    if (swapData.originalOffhand != null && swapData.originalOffhand.getType() != Material.AIR) {
                        // Try to put it back in offhand if it's empty
                        if (inv.getItemInOffHand().getType() == Material.AIR) {
                            inv.setItemInOffHand(swapData.originalOffhand);
                        } else {
                            // Offhand is not empty (shouldn't happen), put it in the original totem slot
                            inv.setItem(swapData.originalTotemSlot, swapData.originalOffhand);
                        }
                    }
                },
                1L
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Clean up if player dies (totem didn't save them)
        swappedItems.remove(event.getEntity().getUniqueId());
    }
}
