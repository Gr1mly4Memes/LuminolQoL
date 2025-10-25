package com.luminol.luminolQoL.utility;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.HashMap;
import java.util.Map;


public final class CropUtils {

    private static final Map<Material, Material> CROP_TO_SEED_MAP = new HashMap<>();
    private static final boolean ISFLAT = ReflectionUtil.supports(13);

    public static boolean isCrop(final Material material) {
        return CROP_TO_SEED_MAP.containsKey(material);
    }

    public static boolean isSeed(final Material material) {
        return CROP_TO_SEED_MAP.containsValue(material);
    }

    public static boolean isFullyGrown(final Block block) {
        if(ISFLAT) {
            if(block.getBlockData() instanceof Ageable ageable) {
                return ageable.getAge() == ageable.getMaximumAge();
            }
        } else {
            try {
                // Annoyingly, if I don't do this then your server will attempt to initialise legacy material support.
                // It's not even getting called on newer versions, so I find it incredibly annoying, it causes a lot of lag.
                final Object state = block.getClass().getMethod("getState").invoke(block);
                final Object data = state.getClass().getMethod("getData").invoke(state);
                return (boolean) data.getClass().getMethod("getData").invoke(data);
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }

        return false;
    }

    public static void setAge(final Block block, final int age) {
        if(ISFLAT) {
            if(block.getBlockData() instanceof Ageable ageable) {
                ageable.setAge(age);
                block.setBlockData(ageable);
            }
        } else {
            try {
                // If not added then your server will attempt to initialise legacy material support.
                // It's not even getting called on newer versions, so it's incredibly annoying, it causes a lot of lag. sometimes.
                final Object state = block.getClass().getMethod("getState").invoke(block);
                final Object data = state.getClass().getMethod("getData").invoke(state);
                data.getClass().getMethod("setData", byte.class).invoke(data, (byte) age);
                state.getClass().getMethod("update", boolean.class).invoke(state, true);
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }
    }

    public static void init() { /* no-op */ }

    static {
        CROP_TO_SEED_MAP.put(XMaterial.WHEAT.get(), XMaterial.WHEAT_SEEDS.get());
        CROP_TO_SEED_MAP.put(XMaterial.CARROTS.get(), XMaterial.CARROT.get());
        CROP_TO_SEED_MAP.put(XMaterial.POTATOES.get(), XMaterial.POTATO.get());
        CROP_TO_SEED_MAP.put(XMaterial.BEETROOTS.get(), XMaterial.BEETROOT.get());
    }

}