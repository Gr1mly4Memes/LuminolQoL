package com.luminol.luminolQoL.utility;

import org.bukkit.ChatColor;

public final class MessageUtil {

    public static String format(final String message) {
        if(message == null)
            return null;

        return ChatColor.translateAlternateColorCodes('&', message);
    }

}