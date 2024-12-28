package com.jamplifier;


import java.util.HashMap;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import com.jamplifier.PlayerData.PlayerManager;

import com.jamplifier.commands.Commands;

public class MainClass extends JavaPlugin {
	
	
	public HashMap<UUID,PlayerManager> playermanager = new HashMap<UUID,PlayerManager>();
	public GameManager gameManager;
	@Override
	public void onEnable() {
	    getLogger().info("ZombieTag has been enabled!");
	    
	    // Initialize gameMechanics here
	    instanceClasses();
	    
	    // Register the events
	    getServer().getPluginManager().registerEvents(new PlayerManager(null, false, false), this);
	    getServer().getPluginManager().registerEvents(gameManager, this);
	    
	    loadConfig();
	    
	    // Register commands
	    getCommand("zombietag").setExecutor(new Commands(this));
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
    	gameManager = new GameManager();
    }
    public GameManager getGameManager() {
        return gameManager;
    }

}
