package com.luminol.luminolQoL.listeners;

import com.luminol.luminolQoL.LuminolQoL;
import com.luminol.luminolQoL.managers.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SitInteractListener implements Listener {

    private final LuminolQoL plugin;
    private final ConfigManager config;

    public SitInteractListener(LuminolQoL plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSit(PlayerInteractEvent e) {
        if (!config.isSitEnabled()) return;

        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null) return;

        BlockData blockData = block.getBlockData();
        Material blockType = block.getType();

        // Check if block is allowed
        List<String> allowed = config.getSitablesBlocks();
        if (!allowed.contains(blockType.toString())) return;

        // Check for stairs or slabs and adjust Y
        double yOffset = 0.0;
        if (blockData instanceof Stairs stairs) {
            if (!stairs.getHalf().equals(Stairs.Half.BOTTOM)) return;
            yOffset = 0.5;
        } else if (blockData instanceof Slab slab) {
            if (!slab.getType().equals(Slab.Type.BOTTOM)) return;
            yOffset = 0.5;
        } else {
            yOffset = 1.0; // full block
        }

        if (player.isSneaking()) return;
        if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) return;
        if (player.isInsideVehicle()) return;

        // Spawn armor stand
        Location loc = block.getLocation().clone().add(0.5, yOffset, 0.5); // center of block

        // Slight push based on facing to prevent clipping
        if (blockData instanceof Directional directional) {
            BlockFace facing = directional.getFacing();
            switch (facing) {
                case SOUTH -> loc.add(0, 0, -0.1);
                case NORTH -> loc.add(0, 0, 0.1);
                case WEST  -> loc.add(0.1, 0, 0);
                case EAST  -> loc.add(-0.1, 0, 0);
            }
        }

        // Spawn invisible, invulnerable armor stand
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setGravity(false);
        stand.setMarker(true); // tiny hitbox
        stand.setMetadata("stair", new FixedMetadataValue(plugin, true));

        // Mount player on armor stand
        stand.addPassenger(player);

        e.setCancelled(true);
    }
}
