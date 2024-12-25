package com.jamplifier;

import org.bukkit.plugin.java.JavaPlugin;

public class ZombieTag extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("ZombieTag has been enabled!");
        // Register commands and events here
    }

    @Override
    public void onDisable() {
        getLogger().info("ZombieTag has been disabled!");
    }
}
