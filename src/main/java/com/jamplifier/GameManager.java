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
    private BukkitRunnable countdownTask = null;


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

        // Start countdown if enough players have joined and the game isn't already started
        if (lobbyPlayers.size() >= playerNeeded && !isStarted) {
            Bukkit.broadcastMessage("§eEnough players! Starting countdown...");
            lobbyCountdown(); // Call countdown function
        } else {
            Bukkit.broadcastMessage("§eWaiting for more players...");
        }
    }





    public void gameStart() {
        // Ensure the game only starts once
        if (isStarted) {
            Bukkit.getLogger().info("Game has already started.");
            return;
        }

        isStarted = true;
        Bukkit.getLogger().info("gameStart() has been called.");

        // Retrieve the game spawn location from the config
        double x = plugin.getConfig().getDouble("GameSpawn.X");
        double y = plugin.getConfig().getDouble("GameSpawn.Y");
        double z = plugin.getConfig().getDouble("GameSpawn.Z");
        String worldName = plugin.getConfig().getString("GameSpawn.world");

        Bukkit.getLogger().info("GameSpawn: World=" + worldName + ", X=" + x + ", Y=" + y + ", Z=" + z);

        if (worldName == null || worldName.isEmpty()) {
            Bukkit.getLogger().severe("Game spawn world is not set in the config!");
            return;
        }

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            Bukkit.getLogger().severe("The world for the game spawn does not exist: " + worldName);
            return;
        }

        Location gameSpawn = new Location(world, x, y, z);
        Bukkit.getLogger().info("Game spawn location created: " + gameSpawn);

        // Create a list for players that will be in the game
        List<Player> gamePlayers = new ArrayList<>();

        // Iterate through all players in the playermanager to handle their state and teleportation
        for (UUID playerUUID : plugin.playermanager.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID); // Get the player by UUID
            if (player == null) {
                Bukkit.getLogger().info("Player with UUID " + playerUUID + " is not online. Skipping.");
                continue;
            }

            PlayerManager playerData = plugin.playermanager.get(playerUUID);
            if (playerData == null) {
                Bukkit.getLogger().info("No PlayerManager data found for " + player.getName() + ". Skipping.");
                continue;
            }

            // Transition players in the lobby to "ingame" state and teleport them
            if (!playerData.isIngame() && !playerData.isIsdead()) {
                playerData.setIngame(true); // Update the state to "ingame"
                plugin.playermanager.put(playerUUID, playerData); // Save the updated data
                player.teleport(gameSpawn); // Teleport to game spawn
                Bukkit.getLogger().info("Teleported player " + player.getName() + " to the game spawn.");

                // Add player to the gamePlayers list
                gamePlayers.add(player);
            } else {
                Bukkit.getLogger().info("Player " + player.getName() + " is already ingame or dead. Skipping.");
            }
        }

        // Update your game players list after the game starts
        plugin.gamePlayers = gamePlayers; // Save this list to your plugin's state (ensure gamePlayers is defined)

        // Announce the game start
        Bukkit.broadcastMessage("§aThe game has started!");
        gameTimer();
    }




    // Lobby countdown logic
    public void lobbyCountdown() {
        int countdownTime = 10; // Set your countdown duration

        countdownTask = new BukkitRunnable() {
            int secondsLeft = countdownTime;

            @Override
            public void run() {
                if (lobbyPlayers.size() < playerNeeded) {
                    Bukkit.broadcastMessage("§cNot enough players! Countdown canceled.");
                    cancelCountdown(); // Cancel the countdown if players leave
                    return;
                }

                if (secondsLeft > 0) {
                    Bukkit.broadcastMessage("§eStarting in " + secondsLeft + " seconds...");
                    secondsLeft--;
                } else {
                    if (!isStarted) {
                        gameStart();
                    }
                    cancel(); // End the countdown
                }
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L); // 0L delay, 20L ticks for 1-second intervals
    }



    // Update the lobby status (e.g., when players join or leave)
    private void updateLobbyStatus() {
        int currentPlayers = lobbyPlayers.size();
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default to 20 if not set
        for (Player lobbyPlayer : lobbyPlayers) {
            lobbyPlayer.sendMessage("§7There are now " + currentPlayers + " out of " + maxPlayers + " players in the lobby.");
        }
    }


    public void removePlayerFromLobby(Player player) {
        // Check if the player is in the lobby
        if (lobbyPlayers.contains(player)) {
            // Remove the player from the lobby
            lobbyPlayers.remove(player);

            // Notify the player
            player.sendMessage("§cYou have left the lobby!");

            // Update the lobby status
            updateLobbyStatus();
            
            // Optionally teleport the player elsewhere (e.g., spawn or another location)
            /*
            Location spawnLocation = new Location(Bukkit.getWorld("world"), 0, 64, 0); // Replace with your spawn location
            player.teleport(spawnLocation);
            */
        } else {
            player.sendMessage("§cYou are not in the lobby!");
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        // Remove the player from the playermanager map
        if (plugin.playermanager.containsKey(playerUUID)) {
            plugin.playermanager.remove(playerUUID);
            plugin.getLogger().info("Removed player " + event.getPlayer().getName() + " from the playermanager.");
            plugin.getGameManager().removePlayerFromLobby(event.getPlayer());
        }
    }
    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null; // Clear the reference
        }
    }
    public void resetGame() {
        // Reset the game state and player data
        isStarted = false;
        gameInProgress = false;
        lobbyPlayers.clear();
        Bukkit.broadcastMessage("§cThe game has ended. Waiting for players to join for the next round.");
    }
    public void gameTimer() {
        int countdownTime = 20; // Set the countdown duration to 60 seconds (1 minute)
        int gracePeriodTime = 10; // Set a grace period (e.g., 50 seconds)
        
        // Add a countdown for the last 10 seconds of game
        countdownTask = new BukkitRunnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (timeLeft > gracePeriodTime) {
                    // This is the grace period, where we wait for the 10-second mark
                    timeLeft--;
                } else if (timeLeft <= gracePeriodTime && timeLeft > 0) {
                    // Once 10 seconds are left, start showing the countdown
                    Bukkit.broadcastMessage("§eTime left: " + timeLeft + " seconds.");
                    timeLeft--; // Decrease the time left
                } else {
                    // Time is up, end the game
                    Bukkit.broadcastMessage("§cThe game time has ended!");
                    endGame(); // Call the endGame function
                    cancel(); // Stop the timer
                }
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L); // 0L delay, 20L ticks for 1-second intervals
    }



    // Methods for checking and changing game state
    public boolean isGameInProgress() {
        return gameInProgress;
    }

    public void endGame() {
        // Example of simple end game logic
    	gameInProgress = false;
        Bukkit.broadcastMessage("§aThe game has ended!");
        
        // Teleport all players to the lobby (or you can customize as needed)
        for (Player player : Bukkit.getOnlinePlayers()) {
            // You could check if the player is in the game, or add custom logic here
            double x = plugin.getConfig().getDouble("LobbySpawn.X");
            double y = plugin.getConfig().getDouble("LobbySpawn.Y");
            double z = plugin.getConfig().getDouble("LobbySpawn.Z");
            String worldName = plugin.getConfig().getString("LobbySpawn.world");

            if (worldName != null && !worldName.isEmpty()) {
                player.teleport(new Location(plugin.getServer().getWorld(worldName), x, y, z));
            }
        }

        // Reset the game state or declare a winner (you can customize this)
        // Reset game flags, player data, etc.
        isStarted = false; // Mark the game as not started
        plugin.playermanager.clear(); // Clear the game players list
    }
    

}

