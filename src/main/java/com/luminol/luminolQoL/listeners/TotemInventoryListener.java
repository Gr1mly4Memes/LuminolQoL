package com.luminol.luminolQoL.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

public class TotemInventoryListener implements Listener {

    public TotemInventoryListener() {
    }

    @EventHandler
    public void onPlayerResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // If player has Totem in offhand, let vanilla handle it
        if (player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;

        // Search entire inventory for a Totem
        int totemIndex = -1;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                totemIndex = i;
                break;
            }
        }

        if (totemIndex != -1) {
            // Consume one Totem
            ItemStack totem = player.getInventory().getItem(totemIndex);
            if (totem.getAmount() > 1) {
                totem.setAmount(totem.getAmount() - 1);
            } else {
                player.getInventory().setItem(totemIndex, null);
            }

            // **Do not cancel the event**, Minecraft will apply the normal Totem effects automatically
            event.setCancelled(false);
        }
        // else do nothing, player dies normally
    }
}
