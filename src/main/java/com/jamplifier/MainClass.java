package com.jamplifier;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.jamplifier.PlayerData.PlayerManager;
import com.jamplifier.PlayerData.StatsManager;
import com.jamplifier.commands.Commands;

public class MainClass extends JavaPlugin {
	
	
	public HashMap<UUID,PlayerManager> playermanager = new HashMap<UUID,PlayerManager>();
	public List<Player> gamePlayers = new ArrayList<>();

	public GameManager gameManager;
	StatsManager statsManager;
	@Override
	public void onEnable() {
	    getLogger().info("ZombieTag has been enabled!");
	    
	    // Initialize gameMechanics here
	    instanceClasses();
	    
	    // Register the events
	    getServer().getPluginManager().registerEvents(new PlayerManager(null, false, false), this);
	    getServer().getPluginManager().registerEvents(gameManager, this);

	    
	    loadConfig();
	 // Initialize the stats folder
	    File statsFolder = new File(getDataFolder(), "stats");
	    if (!statsFolder.exists()) {
	        if (statsFolder.mkdirs()) {
	            getLogger().info("Stats folder created successfully.");
	        } else {
	            getLogger().warning("Failed to create stats folder.");
	        }
	    }

	    // Initialize StatsManager
	    statsManager = new StatsManager(getDataFolder());
	    
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
    public StatsManager getStatsManager() {
        return statsManager;
    }
   
}
