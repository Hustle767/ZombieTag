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
import org.bukkit.metadata.FixedMetadataValue;
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
            player.sendMessage("§cYou do not have permission to join this game.");
            return;
        }

        // Retrieve max players from config
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default to 20 if not set
        int currentPlayers = lobbyPlayers.size(); // Current number of players in the lobby

        // Check if the lobby is full
        if (currentPlayers >= maxPlayers) {
            player.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return;
        }

        // Add player to the lobby
        lobbyPlayers.add(player);
        currentPlayers++; // Increment the player count after adding

        // Broadcast updated lobby status
        Bukkit.broadcastMessage("§a" + player.getName() + " has joined the lobby! (" + currentPlayers + "/" + maxPlayers + ")");

        // Start countdown if enough players have joined and the game isn't already started
        int playersNeeded = getPlayerNeeded(); // Use a method to retrieve the required player count
        if (currentPlayers >= playersNeeded && !isStarted) {
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

        // Retrieve game spawn location from config
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

        // Select and prepare the initial zombie
        Player initialZombie = gamePlayers.get((int) (Math.random() * gamePlayers.size()));
        PlayerManager zombieData = plugin.playermanager.get(initialZombie.getUniqueId());
        if (zombieData != null) {
            zombieData.setIngame(true);
            zombieData.setIsdead(true);

            // Save and replace the current helmet
            zombieData.setOriginalHelmet(initialZombie.getInventory().getHelmet());

            // Get the configured head item
            String headItemType = plugin.getConfig().getString("HeadItem.Type", "PUMPKIN");
            Material headItemMaterial = Material.matchMaterial(headItemType);
            if (headItemMaterial != null) {
                initialZombie.getInventory().setHelmet(new ItemStack(headItemMaterial));
            } else {
                Bukkit.getLogger().severe("Invalid HeadItem.Type in config.yml: " + headItemType);
            }

            initialZombie.teleport(gameSpawn);
            initialZombie.sendMessage("§cYou are the zombie! A grace period is active. Wait to start tagging!");
            startGracePeriod(initialZombie);
        }


        for (Player player : gamePlayers) {
            if (player.equals(initialZombie)) continue;
            player.teleport(gameSpawn);

            PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
            if (playerData != null) {
                playerData.setIngame(true);
            }
        }

        plugin.gamePlayers = gamePlayers;
     
        Bukkit.broadcastMessage("§aThe game has started! Avoid being tagged by the zombie.");
     // Announce game length if enabled in config
        if (plugin.getConfig().getBoolean("AnnounceGameLength", true)) {
            int gameLength = plugin.getConfig().getInt("GameLength", 300); // Default to 300 seconds
            Bukkit.broadcastMessage("§eThe game will last for " + (gameLength / 60) + " minutes.");
        }
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
        int countdownTime = plugin.getConfig().getInt("GameLength", 300); // Default to 5 minutes (300 seconds)
        int gracePeriodTime = 10; // Last 10 seconds of countdown

        // Add a countdown for the game timer
        countdownTask = new BukkitRunnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (timeLeft > gracePeriodTime) {
                    // Normal gameplay, no countdown broadcast
                    timeLeft--;
                } else if (timeLeft <= gracePeriodTime && timeLeft > 0) {
                    // Broadcast countdown for the final seconds
                    Bukkit.broadcastMessage("§eGame ends in " + timeLeft + " seconds.");
                    timeLeft--;
                } else {
                    // Time is up, end the game
                    endGame(); // Call the endGame function
                    cancel(); // Stop the timer
                }
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L); // Schedule with 0L delay and 20 ticks per second
    }


    // Methods for checking and changing game state
    public boolean isGameInProgress() {
        return gameInProgress;
    }
    

    public void endGame() {
        gameInProgress = false;
        Bukkit.broadcastMessage("§aThe game has ended!");
     // Count survivors
        long survivorCount = plugin.playermanager.values().stream()
                .filter(data -> data.isIngame() && !data.isIsdead())
                .count();

     // Fetch win messages from config
        String zombieWinMessage = plugin.getConfig().getString("Messages.ZombieWin", "§cZombies win! All players have been tagged.");
        String survivorWinMessage = plugin.getConfig().getString("Messages.SurvivorWin", "§aSurvivors win! You escaped the zombie apocalypse!");

        if (survivorCount > 0) {
            Bukkit.broadcastMessage(survivorWinMessage.replace("{survivors}", String.valueOf(survivorCount)));
            grantSurvivorRewards();
        } else {
            Bukkit.broadcastMessage(zombieWinMessage);
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
                    playerData.setOriginalHelmet(null); // Clear saved helmet

                    // Teleport player to lobby
                    if (worldName != null && !worldName.isEmpty()) {
                        World world = plugin.getServer().getWorld(worldName);
                        if (world != null) {
                            player.teleport(new Location(world, x, y, z));
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
        
        // Check if the tagging player is in the grace period
        if (taggingPlayer.hasMetadata("gracePeriod")) {
            taggingPlayer.sendMessage("§cYou cannot tag players during the grace period!");
            event.setCancelled(true);
            return;
        }

        PlayerManager taggingData = plugin.playermanager.get(taggingPlayer.getUniqueId());
        PlayerManager taggedData = plugin.playermanager.get(taggedPlayer.getUniqueId());

        // Ensure both players are part of the game
        if (taggingData == null || taggedData == null) return;
        if (!taggingData.isIngame() || !taggedData.isIngame()) return;

        // Check if the tagging player is a zombie
        if (!taggedData.isIsdead()) {
            taggedData.setIsdead(true);

            // Save and replace the current helmet
            if (taggedPlayer.getInventory().getHelmet() != null) {
                taggedData.setOriginalHelmet(taggedPlayer.getInventory().getHelmet());
            }

            // Get the configured head item
            String headItemType = plugin.getConfig().getString("HeadItem.Type", "PUMPKIN");
            Material headItemMaterial = Material.matchMaterial(headItemType);
            if (headItemMaterial != null) {
                taggedPlayer.getInventory().setHelmet(new ItemStack(headItemMaterial));
            } else {
                Bukkit.getLogger().severe("Invalid HeadItem.Type in config.yml: " + headItemType);
            }

            Bukkit.broadcastMessage("§c" + taggedPlayer.getName() + " has been tagged and turned into a zombie!");
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());

        // Check if the player is a zombie (in-game and tagged)
        if (playerData != null && playerData.isIngame() && playerData.isIsdead()) {
            // Prevent removing the helmet
            if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) { // Helmet slot
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem != null && currentItem.getType() == Material.matchMaterial(plugin.getConfig().getString("HeadItem.Type", "PUMPKIN"))) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot remove your helmet while playing!");
                }
            }
        }
    }

    private void startGracePeriod(Player initialZombie) {
        int gracePeriod = plugin.getConfig().getInt("GracePeriod", 10);

        Bukkit.broadcastMessage("§eGrace period started! Zombies cannot tag players for " + gracePeriod + " seconds.");

        // Prevent tagging during grace period
        initialZombie.setMetadata("gracePeriod", new FixedMetadataValue(plugin, true));

        // Schedule the end of the grace period
        new BukkitRunnable() {
            @Override
            public void run() {
                initialZombie.removeMetadata("gracePeriod", plugin);
                initialZombie.sendMessage("§cGrace period is over! Start tagging players.");
            }
        }.runTaskLater(plugin, gracePeriod * 20L); // Grace period duration
    }
    private void grantSurvivorRewards() {
        // Check if the feature is enabled
        if (!plugin.getConfig().getBoolean("SurvivorReward.Enabled", true)) {
            Bukkit.getLogger().info("Survivor rewards are disabled.");
            return;
        }

        // Item Reward Configuration
        String itemType = plugin.getConfig().getString("SurvivorReward.Item.Type", "");
        int itemQuantity = plugin.getConfig().getInt("SurvivorReward.Item.Quantity", 0);
        Material rewardMaterial = Material.matchMaterial(itemType);

        // Command Reward Configuration
        String commandTemplate = plugin.getConfig().getString("SurvivorReward.Command", "");

        // Grant rewards to survivors
        for (Player player : plugin.gamePlayers) {
            PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
            if (playerData != null && !playerData.isIsdead()) {
                // Run command reward
                if (!commandTemplate.isEmpty()) {
                    String command = commandTemplate.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    player.sendMessage("§aA reward has been granted for surviving!");
                }
            }
        }
    }


}

