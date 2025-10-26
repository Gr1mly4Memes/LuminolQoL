package com.luminol.luminolQoL.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Handles vein mining - breaking connected ore blocks when one is broken.
 */
public class VeinMinerListener implements Listener {

    private final JavaPlugin plugin;
    private final Set<Material> veinMineableBlocks;
    private final boolean enabled;
    private final boolean requireSneaking;
    private final int maxBlocks;
    private final int searchRadius;

    // Track blocks being vein mined to prevent infinite loops
    private final Set<Block> processingBlocks = new HashSet<>();

    public VeinMinerListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("vein-miner.enabled", true);
        this.requireSneaking = plugin.getConfig().getBoolean("vein-miner.require-sneaking", true);
        this.maxBlocks = plugin.getConfig().getInt("vein-miner.max-blocks", 50);
        this.searchRadius = plugin.getConfig().getInt("vein-miner.search-radius", 1);

        // Load vein mineable blocks from config
        this.veinMineableBlocks = new HashSet<>();
        List<String> blockList = plugin.getConfig().getStringList("vein-miner.blocks");
        for (String blockName : blockList) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                veinMineableBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid vein miner block: " + blockName);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Prevent infinite loop - if this block is already being processed, skip it
        if (processingBlocks.contains(block)) {
            return;
        }

        // Check if player is in creative mode (skip vein miner in creative)
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Check if sneaking is required
        if (requireSneaking && !player.isSneaking()) {
            return;
        }

        // Check if block is vein mineable
        if (!veinMineableBlocks.contains(block.getType())) {
            return;
        }

        // Check if player is using the correct tool
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isCorrectTool(block.getType(), tool)) {
            return;
        }

        // Find all connected blocks of the same type
        Set<Block> vein = findVein(block, block.getType(), maxBlocks);

        // Remove the original block (already being broken)
        vein.remove(block);

        if (vein.isEmpty()) {
            return;
        }

        // Break all blocks in the vein
        breakVein(vein, player, tool);
    }

    /**
     * Finds all connected blocks of the same type using BFS.
     */
    private Set<Block> findVein(Block start, Material type, int maxBlocks) {
        Set<Block> vein = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && vein.size() < maxBlocks) {
            Block current = queue.poll();
            vein.add(current);

            // Check all adjacent blocks (including diagonals)
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int y = -searchRadius; y <= searchRadius; y++) {
                    for (int z = -searchRadius; z <= searchRadius; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        Block adjacent = current.getRelative(x, y, z);

                        if (!visited.contains(adjacent) && adjacent.getType() == type) {
                            visited.add(adjacent);
                            queue.add(adjacent);
                        }
                    }
                }
            }
        }

        return vein;
    }

    /**
     * Breaks all blocks in the vein with proper drops and tool damage.
     */
    private void breakVein(Set<Block> vein, Player player, ItemStack tool) {
        int durabilityDamage = 0;

        // Add all blocks to processing set to prevent infinite loop
        processingBlocks.addAll(vein);

        try {
            for (Block block : vein) {
                // Double check the block is still the correct type (might have been broken already)
                if (!veinMineableBlocks.contains(block.getType())) {
                    continue;
                }

                // Drop items naturally (respects fortune, silk touch, etc.)
                block.breakNaturally(tool);

                durabilityDamage++;
            }

            // Apply durability damage to tool
            if (tool.getType() != Material.AIR && durabilityDamage > 0) {
                applyDurabilityDamage(tool, player, durabilityDamage);
            }
        } finally {
            // Clean up processing blocks after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                processingBlocks.removeAll(vein);
            }, 1L);
        }
    }

    /**
     * Applies durability damage to the tool.
     */
    private void applyDurabilityDamage(ItemStack tool, Player player, int damage) {
        if (tool.getType().getMaxDurability() == 0) {
            return; // Tool doesn't have durability
        }

        // Check for Unbreaking enchantment
        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        int actualDamage = 0;

        for (int i = 0; i < damage; i++) {
            // Unbreaking has a chance to not consume durability
            if (unbreakingLevel == 0 || Math.random() > (1.0 / (unbreakingLevel + 1))) {
                actualDamage++;
            }
        }

        if (actualDamage > 0) {
            // Use Bukkit's damage method which handles tool breaking
            tool.damage(actualDamage, player);
        }
    }

    /**
     * Checks if the tool is appropriate for the block type.
     */
    private boolean isCorrectTool(Material blockType, ItemStack tool) {
        if (tool == null || tool.getType() == Material.AIR) {
            return false;
        }

        // Check if it's an ore/stone-like block
        if (isOreOrStone(blockType)) {
            return isPickaxe(tool.getType());
        }

        // Check if it's a log/wood block
        if (isLog(blockType)) {
            return isAxe(tool.getType());
        }

        // Check if it's a crop/plant
        if (isCrop(blockType)) {
            return true; // Any tool works for crops
        }

        return true;
    }

    private boolean isOreOrStone(Material material) {
        String name = material.name();
        return name.contains("ORE") || 
               name.contains("STONE") || 
               name.contains("DEEPSLATE") ||
               name.contains("NETHERRACK") ||
               name.contains("ANCIENT_DEBRIS");
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.contains("LOG") || 
               name.contains("WOOD") ||
               name.contains("STEM"); // For crimson/warped stems
    }

    private boolean isCrop(Material material) {
        String name = material.name();
        return name.contains("LEAVES") || 
               name.contains("WART") ||
               material == Material.MELON ||
               material == Material.PUMPKIN;
    }

    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE ||
               material == Material.STONE_PICKAXE ||
               material == Material.IRON_PICKAXE ||
               material == Material.GOLDEN_PICKAXE ||
               material == Material.DIAMOND_PICKAXE ||
               material == Material.NETHERITE_PICKAXE;
    }

    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE ||
               material == Material.STONE_AXE ||
               material == Material.IRON_AXE ||
               material == Material.GOLDEN_AXE ||
               material == Material.DIAMOND_AXE ||
               material == Material.NETHERITE_AXE;
    }
}

