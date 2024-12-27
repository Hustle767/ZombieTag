package com.jamplifier;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class GameManager implements Listener {
	
	private MainClass plugin = MainClass.getPlugin(MainClass.class);

	private int lobbyCountdown = 10;
	private int playerNeeded = 2;
	private boolean isStarted;
	
	private List<Player> lobbyPlayers = new ArrayList<>();
	Location lobbySpawn;
	Location gameSpawn;
	
	
	//Setting up game spawns and lobby spawns
	public void setupGame() {
		
		this.gameSpawn = new Location(Bukkit.getServer().getWorld(plugin.getConfig().getString("GameSpawn.world")),
				plugin.getConfig().getDouble("GameSpawn.X"), plugin.getConfig().getDouble("GameSpawn.Y"),
				plugin.getConfig().getDouble("GameSpawn.Z"));
		
		this.lobbySpawn = new Location(Bukkit.getServer().getWorld(plugin.getConfig().getString("LobbySpawn.world")),
				plugin.getConfig().getDouble("LobbySpawn.X"), plugin.getConfig().getDouble("LobbySpawn.Y"),
				plugin.getConfig().getDouble("LobbySpawn.Z"));
		
		
		
	}
	
	// Lobby wait function
	@SuppressWarnings("deprecation")
	public void lobbyWait(Player player) {
		if (!player.hasPermission("zombietag.lobby.join")) {
		player.sendMessage("&cYou do not have permission to join this game");
		return;
		}
		
		lobbyPlayers.add(player);
		player.teleport(lobbySpawn);
		Bukkit.broadcastMessage("§a" + player.getName() + " has joined the lobby! (" + lobbyPlayers.size() + "/" + playerNeeded + ")");
	    player.sendMessage("§aWaiting for more players...");

	    if (lobbyPlayers.size() >= playerNeeded && !isStarted) {
	    	lobbyCountdown();
	   
	    }
	}
	//Start game function
	public void gameStart() {
        isStarted = true;
        Bukkit.broadcastMessage("§aThe game is starting!");

        for (Player player : lobbyPlayers) {
            player.teleport(gameSpawn);
        }

        lobbyPlayers.clear();
        isStarted = false; // Reset for next game
    }
	//Stop / End game
	public void gameStop() {
		
	}
	
	public void playerCheck() {
		
	}
	// Lobby countdown
    @SuppressWarnings("deprecation")
	public void lobbyCountdown() {
        Bukkit.broadcastMessage("§eGame starting in " + lobbyCountdown + " seconds...");
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int count = lobbyCountdown;

            @SuppressWarnings("deprecation")
			@Override
            public void run() {
                if (count <= 0) {
                    gameStart();
                    Bukkit.getScheduler().cancelTasks(plugin);
                } else {
                    Bukkit.broadcastMessage("§eStarting in " + count + " seconds...");
                    count--;
                }
            }
        }, 0L, 20L); // 20 ticks = 1 second
    }
 
}
