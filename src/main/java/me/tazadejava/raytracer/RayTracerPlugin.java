package me.tazadejava.raytracer;

import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

//main class that registers the command into the server
public class RayTracerPlugin extends JavaPlugin {

    private CommandHandler commandHandler;

    @EventHandler
    public void onEnable() {
        getCommand("raycast").setExecutor(commandHandler = new CommandHandler(this));
        getCommand("raycast").setTabCompleter(commandHandler);
    }

    @EventHandler
    public void onDisable() {
        commandHandler.restoreBlocks(null);
    }
}
