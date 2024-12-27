package com.jamplifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.jamplifier.PlayerData.PlayerManager;

public class GameManager implements Listener {

    private MainClass plugin = MainClass.getPlugin(MainClass.class);
    
    private int lobbyCountdown = 10; // Countdown duration
    private int playerNeeded = plugin.getConfig().getInt("PlayerNeeded", 2); // Retrieve from config
    private boolean isStarted = false;
    private boolean gameInProgress = false; // Track game status

    private List<Player> lobbyPlayers = new ArrayList<>();
    Location lobbySpawn;
    Location gameSpawn;
    
    // Setting up game spawns and lobby spawns
    public void setupGame() {
        this.gameSpawn = new Location(Bukkit.getServer().getWorld(plugin.getConfig().getString("GameSpawn.world")),
                plugin.getConfig().getDouble("GameSpawn.X"), plugin.getConfig().getDouble("GameSpawn.Y"),
                plugin.getConfig().getDouble("GameSpawn.Z"));
        
        this.lobbySpawn = new Location(Bukkit.getServer().getWorld(plugin.getConfig().getString("LobbySpawn.world")),
                plugin.getConfig().getDouble("LobbySpawn.X"), plugin.getConfig().getDouble("LobbySpawn.Y"),
                plugin.getConfig().getDouble("LobbySpawn.Z"));
    }
    
    // Lobby wait function (player joins the lobby)
    public void lobbyWait(Player player) {
        // Permission check to join the lobby
        if (!player.hasPermission("zombietag.lobby.join")) {
            player.sendMessage("§cYou do not have permission to join this game");
            return;
        }

        // Retrieve max players from config
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default is 20 if not set

        // Check if the lobby is full
        if (lobbyPlayers.size() >= maxPlayers) {
            player.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return;
        }

        // Add player to the lobby
        lobbyPlayers.add(player);
        Bukkit.broadcastMessage("§a" + player.getName() + " has joined the lobby! (" + lobbyPlayers.size() + "/" + maxPlayers + ")");
        player.sendMessage("§aWaiting for more players...");

        // Debug logs to check the condition evaluation
        Bukkit.getLogger().info("Current lobby size: " + lobbyPlayers.size() + "/" + maxPlayers);
        Bukkit.getLogger().info("PlayerNeeded is set to: " + playerNeeded);

        // Start countdown if enough players have joined and the game isn't already started
        if (lobbyPlayers.size() >= playerNeeded && !isStarted) {
            Bukkit.broadcastMessage("§eEnough players! Starting countdown...");
            lobbyCountdown(); // Call countdown function
        } else {
            Bukkit.broadcastMessage("§eWaiting for more players...");
        }
    }




    // Start the game (teleport all players to the game spawn)
    public void gameStart() {
        // Ensure the game only starts once
        if (isStarted) {
            return;
        }

        // Set the game as started
        isStarted = true;

        // Example: Retrieve the game spawn location from the config
        double x = plugin.getConfig().getDouble("GameSpawn.X");
        double y = plugin.getConfig().getDouble("GameSpawn.Y");
        double z = plugin.getConfig().getDouble("GameSpawn.Z");
        String worldName = plugin.getConfig().getString("GameSpawn.world");

        if (worldName == null || worldName.isEmpty()) {
            Bukkit.getLogger().severe("Game spawn world is not set in the config!");
            return;
        }

        // Ensure the world exists and is loaded
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            Bukkit.getLogger().severe("The world for the game spawn does not exist: " + worldName);
            return;
        }

        // Load the world and ensure the chunk is loaded
        world.setAutoSave(false); // Disable auto-saving to avoid issues while teleporting players
        world.getChunkAt(new Location(world, x, y, z)).load();

        // Create a location object
        Location gameSpawn = new Location(world, x, y, z);

        // Check if the location is valid
        if (gameSpawn == null || !gameSpawn.getChunk().isLoaded()) {
            Bukkit.getLogger().severe("The game spawn location is invalid or the chunk is not loaded.");
            return;
        }

        // Teleport all players in the game to the game spawn
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.playermanager.containsKey(player.getUniqueId())) {
                PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
                if (playerData.isIngame()) {
                    player.teleport(gameSpawn); // Teleport player to the game spawn location
                    Bukkit.getLogger().info(player.getName() + " has been teleported to the game spawn.");
                }
            }
        }

        // Additional game start logic here (e.g., setting zombies, survivors, etc.)
        Bukkit.broadcastMessage("§aThe game has started!");
    }


    // Lobby countdown logic
    public void lobbyCountdown() {
        int countdownTime = 10; // Set your countdown duration

        // Use BukkitRunnable instead of Runnable to access cancel()
        new BukkitRunnable() {
            int secondsLeft = countdownTime;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    Bukkit.broadcastMessage("§eStarting in " + secondsLeft + " seconds...");
                    secondsLeft--;
                } else {
                    // Make sure the game only starts once
                    if (!isStarted) {
                        Bukkit.broadcastMessage("§aThe game has started!");
                        gameStart();  // Call the gameStart method when the countdown finishes
                    }
                    cancel(); // Cancel the countdown task to prevent it from running further
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 0L delay, 20L ticks for 1 second interval
    }


    // Update the lobby status (e.g., when players join or leave)
    private void updateLobbyStatus() {
        int currentPlayers = lobbyPlayers.size();
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default to 20 if not set
        for (Player lobbyPlayer : lobbyPlayers) {
            lobbyPlayer.sendMessage("§7There are now " + currentPlayers + " out of " + maxPlayers + " players in the lobby.");
        }
    }
    public void resetGame() {
        // Reset the game state and player data
        isStarted = false;
        gameInProgress = false;
        lobbyPlayers.clear();
        Bukkit.broadcastMessage("§cThe game has ended. Waiting for players to join for the next round.");
    }


    // Methods for checking and changing game state
    public boolean isGameInProgress() {
        return gameInProgress;
    }

    public void startGame() {
        gameInProgress = true;
    }

    public void endGame() {
        gameInProgress = false;
    }
}

