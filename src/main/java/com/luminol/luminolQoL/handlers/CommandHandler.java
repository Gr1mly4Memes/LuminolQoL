package com.luminol.luminolQoL.handlers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

public class CommandHandler {

    public static void register(Command... cmds) {
        CommandMap commandMap;
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (Command cmd : cmds) {
            commandMap.register(cmd.getName(), cmd);
        }
    }
}
