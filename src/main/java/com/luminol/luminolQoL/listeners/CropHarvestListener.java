package com.luminol.luminolQoL.listeners;

import com.cryptomorin.xseries.XSound;
import com.luminol.luminolQoL.managers.ConfigManager;
import com.luminol.luminolQoL.utility.CropUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;

public class CropHarvestListener implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public CropHarvestListener(final JavaPlugin plugin, final ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCropHarvest(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final Material type = block.getType();

        if(player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        if(!CropUtils.isCrop(type) || !CropUtils.isFullyGrown(block)) {
            return;
        }

        final ItemStack tool = player.getInventory().getItemInMainHand();
        final Collection<ItemStack> drops = block.getDrops(tool);

        final World world = block.getWorld();
        final Location location = block.getLocation().clone().add(0.5D, 0.5D, 0.5D);

        for(final ItemStack drop : new ArrayList<>(drops)) {
            if(CropUtils.isSeed(drop.getType())) {
                if(drop.getAmount() > 1) drop.setAmount(drop.getAmount() - 1);
                else drops.remove(drop);
                break;
            }
        }

        for(final ItemStack drop : drops) {
            world.dropItemNaturally(location, drop);
        }

        event.setCancelled(true);
        CropUtils.setAge(block, 0);

        if(this.configManager.shouldPlaySoundEffects()) {
            final String soundSource = this.configManager.getSoundSource();
            final XSound.Record record = XSound.parse(soundSource);

            if(record != null) {
                record.withVolume(this.configManager.getSoundVolume());
                record.withPitch(this.configManager.getSoundPitch());

                final XSound.SoundPlayer sound = record.soundPlayer().forPlayers(player);
                sound.play();
            } else {
                this.plugin.getLogger().warning(String.format("The sound '%s' does not exist.", soundSource));
            }
        }
    }

}