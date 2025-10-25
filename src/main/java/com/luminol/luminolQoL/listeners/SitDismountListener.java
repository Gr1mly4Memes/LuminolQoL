package com.luminol.luminolQoL.listeners;

import com.luminol.luminolQoL.LuminolQoL;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

public class SitDismountListener implements Listener {

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getDismounted().hasMetadata("stair")) {
            Bukkit.getScheduler().runTaskLater(LuminolQoL.getPlugin(LuminolQoL.class),
                    () -> e.getDismounted().remove(), 1L);
        }
    }
}
