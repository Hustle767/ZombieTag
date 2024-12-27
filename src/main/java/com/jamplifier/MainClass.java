package com.jamplifier;


import java.util.HashMap;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import com.jamplifier.PlayerData.PlayerManager;
import com.jamplifier.commands.Admin;

public class MainClass extends JavaPlugin {
	
	
	public HashMap<UUID,PlayerManager> playermanager = new HashMap<UUID,PlayerManager>();
	public GameMechanics gameMechanics;
    @Override
    public void onEnable() {
        getLogger().info("ZombieTag has been enabled!");
        getServer().getPluginManager().registerEvents(new GameMechanics(), this);
        loadConfig();
        // Register commands and events here
        
        getCommand("zombietag").setExecutor(new Admin(this));

    }

    @Override
    public void onDisable() {
        getLogger().info("ZombieTag has been disabled!");
    }
    
    public void loadConfig() {
    	getConfig().options().copyDefaults(true);
    	saveConfig();
    	
    }
    
    public void instanceClasses() {
    	gameMechanics = new GameMechanics();
    }
}
