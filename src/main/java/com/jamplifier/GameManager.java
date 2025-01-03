package com.jamplifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.jamplifier.PlayerData.PlayerManager;

public class GameManager implements Listener {

    private MainClass plugin = MainClass.getPlugin(MainClass.class);
    
    private int lobbyCountdown = 10; // Countdown duration
    private boolean isStarted = false;
    private boolean gameInProgress = false; // Track game status
    private BukkitRunnable countdownTask = null;


    public List<Player> lobbyPlayers = new ArrayList<>();
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
        if (lobbyPlayers.size() >= getPlayerNeeded() && !isStarted) {
            Bukkit.broadcastMessage("§eEnough players! Starting countdown...");
            lobbyCountdown(); // Call countdown function
        } else {
            Bukkit.broadcastMessage("§eWaiting for more players...");
        }
    }





    public void gameStart() {
        if (isStarted) {
            Bukkit.getLogger().info("Game has already started.");
            return;
        }

        isStarted = true;
        gameInProgress = true;

        // Retrieve the game spawn location
        double x = plugin.getConfig().getDouble("GameSpawn.X");
        double y = plugin.getConfig().getDouble("GameSpawn.Y");
        double z = plugin.getConfig().getDouble("GameSpawn.Z");
        String worldName = plugin.getConfig().getString("GameSpawn.world");

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

        List<Player> gamePlayers = new ArrayList<>(lobbyPlayers);

        // Randomly select the initial zombie
     // Inside gameStart()
        Player initialZombie = gamePlayers.get((int) (Math.random() * gamePlayers.size()));
        PlayerManager zombieData = plugin.playermanager.get(initialZombie.getUniqueId());
        if (zombieData != null) {
            zombieData.setIngame(true);
            zombieData.setIsdead(true); // Mark as zombie

            // Save the current helmet
            zombieData.setOriginalHelmet(initialZombie.getInventory().getHelmet());

            // Equip the pumpkin helmet
            initialZombie.getInventory().setHelmet(new ItemStack(Material.PUMPKIN));
            initialZombie.sendMessage("§cYou are the zombie! Tag other players to turn them into zombies.");
            
            // Teleport the zombie to the game spawn
            initialZombie.teleport(gameSpawn);
        }


        // Update all other players
        for (Player player : gamePlayers) {
            if (player.equals(initialZombie)) continue; // Zombie already teleported
            player.teleport(gameSpawn);
            plugin.playermanager.get(player.getUniqueId()).setIngame(true);
        }

        plugin.gamePlayers = gamePlayers; // Save game players
        Bukkit.broadcastMessage("§aThe game has started! Avoid being tagged by the zombie.");
        gameTimer();
    }




    // Lobby countdown logic
    public void lobbyCountdown() {
        countdownTask = new BukkitRunnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                if (lobbyPlayers.size() < getPlayerNeeded()) {
                    Bukkit.broadcastMessage("§cNot enough players! Countdown canceled.");
                    cancelCountdown();
                    return;
                }

                if (secondsLeft > 0) {
                    Bukkit.broadcastMessage("§eStarting in " + secondsLeft + " seconds...");
                    secondsLeft--;
                } else {
                    gameStart();
                    cancel();
                }
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    private int getPlayerNeeded() {
        return plugin.getConfig().getInt("PlayerNeeded", 2);
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
        int countdownTime = 20;

        countdownTask = new BukkitRunnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (plugin.gamePlayers.isEmpty()) {
                    Bukkit.broadcastMessage("§cNo players left in the game. Ending the game.");
                    endGame();
                    cancel();
                    return;
                }

                if (timeLeft > 0) {
                    for (Player player : plugin.gamePlayers) {
                        player.sendMessage("§eTime left: " + timeLeft + " seconds.");
                    }
                    timeLeft--;
                } else {
                    Bukkit.broadcastMessage("§cThe game time has ended!");
                    endGame();
                    cancel();
                }
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }



    // Methods for checking and changing game state
    public boolean isGameInProgress() {
        return gameInProgress;
    }
    

    public void endGame() {
        gameInProgress = false;

        // Count survivors
        long survivorCount = plugin.playermanager.values().stream()
                .filter(data -> data.isIngame() && !data.isIsdead())
                .count();

        // Announce the winner
        if (survivorCount > 0) {
            Bukkit.broadcastMessage("§aSurvivors win! " + survivorCount + " player(s) survived the zombie apocalypse.");
        } else {
            Bukkit.broadcastMessage("§cZombies win! All players have been tagged.");
        }

        // Retrieve lobby spawn location
        double x = plugin.getConfig().getDouble("LobbySpawn.X");
        double y = plugin.getConfig().getDouble("LobbySpawn.Y");
        double z = plugin.getConfig().getDouble("LobbySpawn.Z");
        String worldName = plugin.getConfig().getString("LobbySpawn.world");

        for (UUID playerId : plugin.playermanager.keySet()) {
            PlayerManager playerData = plugin.playermanager.get(playerId);
            if (playerData != null && playerData.isIngame()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    // Restore original helmet
                    player.getInventory().setHelmet(playerData.getOriginalHelmet());
                    playerData.setOriginalHelmet(null);

                    // Teleport player to lobby
                    if (worldName != null && !worldName.isEmpty()) {
                        World world = plugin.getServer().getWorld(worldName);
                        if (world != null) {
                            player.teleport(new Location(world, x, y, z));
                            player.sendMessage("§aYou have been teleported to the lobby.");
                        } else {
                            player.sendMessage("§cThe lobby world does not exist.");
                        }
                    }
                }
                // Reset player state
                playerData.setIngame(false);
                playerData.setIsdead(false);
            }
        }

        // Clear game state
        isStarted = false;
        plugin.gamePlayers.clear();
        lobbyPlayers.clear(); // Ensure lobby is emptied
        plugin.playermanager.clear(); // Clear all player data
    }



    @EventHandler
    public void onPlayerTag(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player taggedPlayer = (Player) event.getEntity();
        Player taggingPlayer = (Player) event.getDamager();

        PlayerManager taggingData = plugin.playermanager.get(taggingPlayer.getUniqueId());
        PlayerManager taggedData = plugin.playermanager.get(taggedPlayer.getUniqueId());

        // Ensure both players are part of the game
        if (taggingData == null || taggedData == null) return;
        if (!taggingData.isIngame() || !taggedData.isIngame()) return;

        // Check if the tagging player is a zombie
        if (taggingData.isIsdead()) { // Tagging player is a zombie
            if (!taggedData.isIsdead()) { // Tagged player is not yet a zombie
                // Save the original helmet before tagging
                if (taggedPlayer.getInventory().getHelmet() != null) {
                    taggedData.setOriginalHelmet(taggedPlayer.getInventory().getHelmet());
                }

                // Mark the player as a zombie and equip the pumpkin
                taggedData.setIsdead(true);
                taggedPlayer.getInventory().setHelmet(new ItemStack(Material.PUMPKIN));
                Bukkit.broadcastMessage("§c" + taggedPlayer.getName() + " has been tagged and turned into a zombie!");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());

        // Check if the player is a zombie and in-game
        if (playerData != null && playerData.isIngame() && playerData.isIsdead()) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) { // Helmet slot
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem != null && currentItem.getType() == Material.PUMPKIN) {
                    event.setCancelled(true); // Prevent removing the pumpkin
                    player.sendMessage("§cYou cannot remove your helmet while playing!");
                }
            }
        }
    }


}

