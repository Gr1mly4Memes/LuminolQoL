package com.luminol.luminolQoL.utility;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Based off of XReflection.
public class ReflectionUtil {

    public static final int MAJOR_NUMBER;
    public static final int MINOR_NUMBER;
    public static final int PATCH_NUMBER;

    static {
        final Matcher version = Pattern
                .compile("^(?<major>\\d+)\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?")
                .matcher(Bukkit.getBukkitVersion());

        if (version.find()) {
            try {
                String patch = version.group("patch");
                MAJOR_NUMBER = Integer.parseInt(version.group("major"));
                MINOR_NUMBER = Integer.parseInt(version.group("minor"));
                PATCH_NUMBER = Integer.parseInt((patch == null || patch.isEmpty()) ? "0" : patch);
            } catch (final Throwable exception) {
                throw new IllegalStateException("Failed to parse minor number: " + version, exception);
            }
        } else {
            throw new IllegalStateException("Cannot parse server version: \"" + Bukkit.getBukkitVersion() + '"');
        }
    }

    public static boolean supports(int minorNumber) {
        return MINOR_NUMBER >= minorNumber;
    }

}