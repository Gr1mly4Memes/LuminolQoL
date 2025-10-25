package com.luminol.luminolQoL.managers;

import com.luminol.luminolQoL.LuminolQoL;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {

    private final LuminolQoL plugin;
    private FileConfiguration config;
    private File configFile;

    // --- AutoReplant ---
    private boolean autoReplantEnabled;
    private String permissionMessage;
    private String reloadMessage;
    private boolean playSoundEffects;
    private String soundSource;
    private float soundVolume;
    private float soundPitch;

    // --- Sitting ---
    private boolean sitEnabled;
    private List<String> sitablesBlocks;

    public ConfigManager(LuminolQoL plugin) {
        this.plugin = plugin;
    }

    /** Load or create config */
    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }

    /** Reload config */
    public void reload() {
        load();
    }

    /** Load values from config.yml */
    private void loadValues() {
        // --- AutoReplant ---
        autoReplantEnabled = config.getBoolean("autoReplantEnabled", true);
        permissionMessage = config.getString("messages.permission", "&cYou do not have permission.");
        reloadMessage = config.getString("messages.reload", "&aConfig reloaded!");
        playSoundEffects = config.getBoolean("autoReplant.play-sound-effects", true);
        soundSource = config.getString("autoReplant.sound-source", "ENTITY_ITEM_PICKUP");
        soundVolume = (float) config.getDouble("autoReplant.sound-volume", 1.0);
        soundPitch = (float) config.getDouble("autoReplant.sound-pitch", 1.0);

        // --- Sitting ---
        sitEnabled = config.getBoolean("sit.enabled", true);
        sitablesBlocks = config.getStringList("sitables.blocks");
    }

    /** Save current values back to config.yml */
    public void save() {
        // --- AutoReplant ---
        config.set("autoReplantEnabled", autoReplantEnabled);
        config.set("messages.permission", permissionMessage);
        config.set("messages.reload", reloadMessage);
        config.set("autoReplant.play-sound-effects", playSoundEffects);
        config.set("autoReplant.sound-source", soundSource);
        config.set("autoReplant.sound-volume", soundVolume);
        config.set("autoReplant.sound-pitch", soundPitch);

        // --- Sitting ---
        config.set("sit.enabled", sitEnabled);
        config.set("sitables.blocks", sitablesBlocks);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml!");
            e.printStackTrace();
        }
    }

    // --- AutoReplant getters/setters ---
    public boolean isAutoReplantEnabled() { return autoReplantEnabled; }
    public void setAutoReplantEnabled(boolean value) { autoReplantEnabled = value; save(); }

    public String getPermissionMessage() { return permissionMessage; }
    public void setPermissionMessage(String message) { permissionMessage = message; save(); }

    public String getReloadMessage() { return reloadMessage; }
    public void setReloadMessage(String message) { reloadMessage = message; save(); }

    public boolean shouldPlaySoundEffects() { return playSoundEffects; }
    public void setPlaySoundEffects(boolean value) { playSoundEffects = value; save(); }

    public String getSoundSource() { return soundSource; }
    public void setSoundSource(String value) { soundSource = value; save(); }

    public float getSoundVolume() { return soundVolume; }
    public void setSoundVolume(float value) { soundVolume = value; save(); }

    public float getSoundPitch() { return soundPitch; }
    public void setSoundPitch(float value) { soundPitch = value; save(); }

    // --- Sitting getters/setters ---
    public boolean isSitEnabled() { return sitEnabled; }
    public void setSitEnabled(boolean value) { sitEnabled = value; save(); }

    public List<String> getSitablesBlocks() { return sitablesBlocks; }
    public void setSitablesBlocks(List<String> list) { sitablesBlocks = list; save(); }
}
