package com.jamplifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import com.jamplifier.PlayerData.PlayerManager;

public class GameManager implements Listener {

    /*** GAME STATE TRACKING ***/
    private MainClass plugin = MainClass.getPlugin(MainClass.class);
    private boolean isStarted = false;
    private boolean gameInProgress = false;
    private BukkitRunnable countdownTask = null;
    private boolean countdownRunning = false;
    private Player initialZombie;
    int gracePeriod = plugin.getConfig().getInt("GracePeriod", 10);

    /*** SPAWN LOCATIONS ***/
    private Location lobbySpawn;
    private Location gameSpawn;
    
    /*** LOBBY PLAYERS ***/
    public List<Player> lobbyPlayers = new ArrayList<>();
    public List<Player> getLobbyPlayers() {
        return lobbyPlayers;
    }

    /*** GETTERS & SETTERS ***/
    public Player getInitialZombie() {
        return initialZombie;
    }

    public void setInitialZombie(Player zombie) {
        this.initialZombie = zombie;
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }
    
    public boolean isPlayerInLobby(Player player) {
        return lobbyPlayers.contains(player);
    }


    private int getPlayerNeeded() {
        return plugin.getConfig().getInt("PlayerNeeded", 2);
    }
    private final Map<Player, Location> lastLocationMap = new HashMap<>();
    private final Map<Player, Integer> stayStillWarnings = new HashMap<>();
    private final Map<UUID, BukkitRunnable> stayStillTimers = new HashMap<>();

    private BukkitRunnable stayStillTask;

    /*** SPAWN LOCATIONS SETUP ***/
    public void setupGame() {
        World gameWorld = Bukkit.getServer().getWorld(plugin.getConfig().getString("GameSpawn.world"));
        World lobbyWorld = Bukkit.getServer().getWorld(plugin.getConfig().getString("LobbySpawn.world"));

        if (gameWorld != null) {
            this.gameSpawn = new Location(
                    gameWorld,
                    plugin.getConfig().getDouble("GameSpawn.X"),
                    plugin.getConfig().getDouble("GameSpawn.Y"),
                    plugin.getConfig().getDouble("GameSpawn.Z"),
                    (float) plugin.getConfig().getDouble("GameSpawn.Yaw", 0),
                    (float) plugin.getConfig().getDouble("GameSpawn.Pitch", 0)
            );
        } else {
            plugin.getLogger().severe("Game world is invalid!");
        }

        if (lobbyWorld != null) {
            this.lobbySpawn = new Location(
                    lobbyWorld,
                    plugin.getConfig().getDouble("LobbySpawn.X"),
                    plugin.getConfig().getDouble("LobbySpawn.Y"),
                    plugin.getConfig().getDouble("LobbySpawn.Z"),
                    (float) plugin.getConfig().getDouble("LobbySpawn.Yaw", 0),
                    (float) plugin.getConfig().getDouble("LobbySpawn.Pitch", 0)
            );
        } else {
            plugin.getLogger().severe("Lobby world is invalid!");
        }
    }

    /*** LOBBY MANAGEMENT ***/
    public void lobbyWait(Player player) {
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default is 20 if not set
        int currentPlayers = lobbyPlayers.size();

        if (currentPlayers >= maxPlayers) {
            player.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return;
        }

        lobbyPlayers.add(player);
        currentPlayers++;

        for (Player lobbyPlayer : lobbyPlayers) {
            lobbyPlayer.sendMessage("§a" + player.getName() + " has joined the lobby! (" + currentPlayers + "/" + maxPlayers + ")");
        }

        // Start countdown if enough players have joined and it isn’t already running
        if (currentPlayers >= getPlayerNeeded() && !isStarted && !countdownRunning) {
            lobbyCountdown();
        } else {
            for (Player lobbyPlayer : lobbyPlayers) {
                lobbyPlayer.sendMessage("§eWaiting for more players...");
            }
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
    
    private void updateLobbyStatus() {
        int currentPlayers = lobbyPlayers.size();
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default to 20 if not set
        for (Player lobbyPlayer : lobbyPlayers) {
            lobbyPlayer.sendMessage("§7There are now " + currentPlayers + " out of " + maxPlayers + " players in the lobby.");
        }
    }
    
    public void lobbyCountdown() {
        if (countdownRunning) {
            Bukkit.getLogger().info("Countdown is already running.");
            return;
        }

        countdownRunning = true;

        countdownTask = new BukkitRunnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                if (lobbyPlayers.size() < getPlayerNeeded()) {
                    for (Player lobbyPlayer : lobbyPlayers) {
                        lobbyPlayer.sendMessage("§cNot enough players! Countdown canceled.");
                    }
                    cancelCountdown();
                    return;
                }

                if (secondsLeft > 0) {
                    for (Player lobbyPlayer : lobbyPlayers) {
                        lobbyPlayer.sendMessage("§eStarting in " + secondsLeft + " seconds...");
                    }
                    secondsLeft--;
                } else {
                    gameStart();
                    cancel();
                }
            }

            @Override
            public void cancel() {
                super.cancel();
                countdownRunning = false; // Reset the countdown state
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L); // Schedule with 0L delay and 20 ticks per second
    }
    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null; // Clear the reference
        }
    }

    /*** GAME START & FLOW ***/
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
        float yaw = (float) plugin.getConfig().getDouble("GameSpawn.Yaw", 0); // Default yaw 0
        float pitch = (float) plugin.getConfig().getDouble("GameSpawn.Pitch", 0); // Default pitch 0
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

        Location gameSpawn = new Location(world, x, y, z, yaw, pitch);

        List<Player> gamePlayers = new ArrayList<>(lobbyPlayers);

        // Select and prepare the initial zombie
        Player initialZombie = gamePlayers.get((int) (Math.random() * gamePlayers.size()));
        PlayerManager zombieData = plugin.playermanager.get(initialZombie.getUniqueId());
        if (zombieData != null) {
            zombieData.setIngame(true);
            zombieData.setIsdead(true);

            // Save and replace the current helmet
            saveHelmet(initialZombie);

            // Get the configured head item
            String headItemType = plugin.getConfig().getString("HeadItem.Type", "PUMPKIN");
            Material headItemMaterial = Material.matchMaterial(headItemType);
            if (headItemMaterial != null) {
                initialZombie.getInventory().setHelmet(new ItemStack(headItemMaterial));
            } else {
                Bukkit.getLogger().severe("Invalid HeadItem.Type in config.yml: " + headItemType);
            }

            // Apply simulated darkness effect
            applySuspiciousStewBlindness(initialZombie);
            initialZombie.teleport(gameSpawn);
            initialZombie.sendMessage("§cYou are the zombie! A grace period is active. Wait to start tagging!");
            initialZombie.sendActionBar("§cYou are the ZOMBIE!");
            initialZombie.sendTitle(
                "§cYou are the ZOMBIE!!", // Title
                "§eTag others till there are no survivors!", // Subtitle
                10, // Fade-in duration (ticks)
                70, // Stay duration (ticks)
                20  // Fade-out duration (ticks)
            );

            startGracePeriod(initialZombie);
        }

        // Save helmets for all other players and teleport them
        for (Player player : gamePlayers) {
            if (player.equals(initialZombie)) continue;

            // Save the helmet for each player without replacing it
            String serializedHelmet = plugin.getStatsManager().serializeHelmet(player.getInventory().getHelmet());
            plugin.getStatsManager().updatePlayerStat(player.getUniqueId(), "originalHelmet", serializedHelmet);

            // Teleport to game spawn
            player.teleport(gameSpawn);

            PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
            if (playerData != null) {
                playerData.setIngame(true);
            }
        }

        plugin.gamePlayers = gamePlayers;

        for (Player gamePlayer : plugin.gamePlayers) {
            gamePlayer.sendMessage("§aThe game has started! The grace period will last " + gracePeriod + " seconds. Avoid being tagged by the zombie.");
        }

        // Announce game length if enabled in config
        if (plugin.getConfig().getBoolean("AnnounceGameLength", true)) {
            int gameLength = plugin.getConfig().getInt("GameLength", 300); // Default to 300 seconds
            for (Player gamePlayer : plugin.gamePlayers) {
                gamePlayer.sendMessage("§eThe game will last for " + (gameLength / 60) + " minutes.");
            }
        }

        // Start tracking for the stay-still feature
        startStayStillTimer();
        gameTimer();
    }

    private void startGracePeriod(Player initialZombie) {


        // Notify all players about the start of the grace period
        for (Player gamePlayer : plugin.gamePlayers) {
            gamePlayer.sendMessage("§eGrace period started! Zombies cannot tag players for " + gracePeriod + " seconds.");
        }

        // Prevent tagging during grace period for the initial zombie
        initialZombie.setMetadata("gracePeriod", new FixedMetadataValue(plugin, true));

        // Schedule the end of the grace period
        new BukkitRunnable() {
            @Override
            public void run() {
                initialZombie.removeMetadata("gracePeriod", plugin);

                // Notify all players about the end of the grace period
                for (Player gamePlayer : plugin.gamePlayers) {
                    gamePlayer.sendMessage("§cGrace period is over! Zombies can now tag players!");
                }
            }
        }.runTaskLater(plugin, gracePeriod * 20L); // Grace period duration in ticks
    }

    public void applySuspiciousStewBlindness(Player player) {
        int blindnessSeconds = plugin.getConfig().getInt("Effects.BlindnessDuration", 10); // Default: 10
        int nightVisionSeconds = plugin.getConfig().getInt("Effects.NightVisionDuration", 10); // Default: 10
        // Convert seconds to ticks
        int blindnessTicks = blindnessSeconds * 20;
        int nightVisionTicks = nightVisionSeconds * 20;

        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, nightVisionTicks, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessTicks, 0));
    }

    public void gameTimer() {
        if (!gameInProgress) {
            Bukkit.getLogger().warning("Game timer attempted to start but the game is not in progress.");
            return;
        }

        int countdownTime = plugin.getConfig().getInt("GameLength", 300); // Default to 300 seconds
        int gracePeriodTime = 10; // Last 10 seconds of countdown

        Bukkit.getLogger().info("Starting game timer for " + countdownTime + " seconds.");

        countdownTask = new BukkitRunnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (!gameInProgress) {
                    Bukkit.getLogger().warning("Game timer detected that the game is no longer in progress. Cancelling timer.");
                    cancel();
                    return;
                }

                if (timeLeft > gracePeriodTime) {
                    // Normal gameplay, no countdown broadcast
                    timeLeft--;
                } else if (timeLeft <= gracePeriodTime && timeLeft > 0) {
                    // Broadcast countdown for the final seconds
                    for (Player gamePlayer : plugin.gamePlayers) {
                        gamePlayer.sendMessage("§eGame ends in " + timeLeft + " seconds.");
                    }
                    timeLeft--;
                } else {
                    // Time is up, end the game
                    Bukkit.getLogger().info("Game timer completed. Ending the game.");
                    endGame();
                    cancel();
                }
            }
        };

        countdownTask.runTaskTimer(plugin, 0L, 20L); // Schedule with 0L delay and 20 ticks per second
    }

    /*** GAME END & RESET ***/
    public void endGame() {
        gameInProgress = false;

        for (Player gamePlayer : plugin.gamePlayers) {
            gamePlayer.sendMessage("§aThe game has ended!");
        }

        // Count survivors
        long survivorCount = plugin.playermanager.values().stream()
                .filter(data -> data.isIngame() && !data.isIsdead())
                .count();

        // Fetch win messages from config
        String zombieWinMessage = plugin.getConfig().getString("Messages.ZombieWin", "§cZombies win! All players have been tagged.");
        String survivorWinMessage = plugin.getConfig().getString("Messages.SurvivorWin", "§aSurvivors win! You escaped the zombie apocalypse!");

        if (survivorCount > 0) {
            for (Player gamePlayer : plugin.gamePlayers) {
                gamePlayer.sendMessage(survivorWinMessage);
            }

            // Update survivor stats
            plugin.playermanager.values().stream()
                    .filter(data -> data.isIngame() && !data.isIsdead())
                    .forEach(data -> plugin.getStatsManager().updatePlayerStat(data.getUuid(), "survivals", 1));

            grantSurvivorRewards();
        } else {
            for (Player gamePlayer : plugin.gamePlayers) {
                gamePlayer.sendMessage(zombieWinMessage);
            }
        }

        // Retrieve lobby spawn location
        double x = plugin.getConfig().getDouble("LobbySpawn.X");
        double y = plugin.getConfig().getDouble("LobbySpawn.Y");
        double z = plugin.getConfig().getDouble("LobbySpawn.Z");
        float yaw = (float) plugin.getConfig().getDouble("LobbySpawn.Yaw", 0); // Default yaw 0
        float pitch = (float) plugin.getConfig().getDouble("LobbySpawn.Pitch", 0); // Default pitch 0
        String worldName = plugin.getConfig().getString("LobbySpawn.world");

        World world = (worldName != null && !worldName.isEmpty()) ? plugin.getServer().getWorld(worldName) : null;
        Location lobbySpawn = (world != null) ? new Location(world, x, y, z, yaw, pitch) : null;

        // Preserve players who were queued for the next game
        List<Player> queuedPlayers = new ArrayList<>(lobbyPlayers);

        // Restore helmets and reset player states
        for (UUID playerId : plugin.playermanager.keySet()) {
            PlayerManager playerData = plugin.playermanager.get(playerId);
            if (playerData != null && playerData.isIngame()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    // Restore the player's original helmet
                    restoreHelmet(player);

                    // Teleport player to lobby if location exists
                    if (lobbySpawn != null) {
                        player.teleport(lobbySpawn);
                    } else {
                        player.sendMessage("§cThe lobby world does not exist.");
                    }
                }
                // Reset player state
                playerData.setIngame(false);
                playerData.setIsdead(false);
            }
        }

        isStarted = false;

     // Remove only players who were in the game from playermanager (Keep queued players)
        plugin.playermanager.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            return player != null && plugin.gamePlayers.contains(player);
        });

        // Remove only players who were in the game from the lobby (Keep queued players)
        lobbyPlayers.removeIf(player -> plugin.gamePlayers.contains(player));

        // Ensure only queued players remain in the lobby
        plugin.gamePlayers.clear();

        // Stop all timers
        cancelStayStillTimer();


    }
    public void resetGame() {
        Bukkit.getLogger().info("Resetting the game state.");
        isStarted = false;
        gameInProgress = false;

        lobbyPlayers.clear();
        plugin.gamePlayers.clear();

        for (PlayerManager playerData : plugin.playermanager.values()) {
            playerData.setIngame(false);
            playerData.setIsdead(false);
            Player player = Bukkit.getPlayer(playerData.getUuid());
            if (player != null && playerData.getOriginalHelmet() != null) {
                player.getInventory().setHelmet(playerData.getOriginalHelmet());
                playerData.setOriginalHelmet(null);
            }
        }

        plugin.playermanager.clear();
        for (Player gamePlayer : plugin.gamePlayers) {
            gamePlayer.sendMessage("§cThe game has ended. Waiting for players to join for the next round.");
        }
    }

    private void endGameAndTeleportAll() {
        Bukkit.getLogger().info("Ending game and teleporting all players to the lobby.");

        String worldName = plugin.getConfig().getString("LobbySpawn.world");
        double x = plugin.getConfig().getDouble("LobbySpawn.X");
        double y = plugin.getConfig().getDouble("LobbySpawn.Y");
        double z = plugin.getConfig().getDouble("LobbySpawn.Z");
        float yaw = (float) plugin.getConfig().getDouble("LobbySpawn.Yaw", 0); // Default yaw 0
        float pitch = (float) plugin.getConfig().getDouble("LobbySpawn.Pitch", 0); // Default pitch 0

        World world = (worldName != null && !worldName.isEmpty()) ? plugin.getServer().getWorld(worldName) : null;
        if (world == null) {
            Bukkit.getLogger().severe("Lobby spawn world is invalid or not set: " + worldName);
            return;
        }

        Location lobbySpawn = new Location(world, x, y, z, yaw, pitch);

        for (Player gamePlayer : new ArrayList<>(plugin.gamePlayers)) {
            PlayerManager playerData = plugin.playermanager.get(gamePlayer.getUniqueId());
            if (playerData != null) {
                playerData.setIngame(false);
                playerData.setIsdead(false);
                restoreHelmet(gamePlayer);
            }

            gamePlayer.teleport(lobbySpawn);
            gamePlayer.sendMessage("§aYou have been returned to the lobby.");
        }

        resetGame();
    }

    private void grantSurvivorRewards() {
        // Check if the feature is enabled
        if (!plugin.getConfig().getBoolean("SurvivorReward.Enabled", true)) {
            Bukkit.getLogger().info("Survivor rewards are disabled.");
            return;
        }

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

    /*** PLAYER INTERACTION (TAGGING & EFFECTS) ***/
    @EventHandler
    public void onPlayerTag(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player taggedPlayer = (Player) event.getEntity();
        Player taggingPlayer = (Player) event.getDamager();

        // Prevent tagging during grace period
        if (taggingPlayer.hasMetadata("gracePeriod")) {
            taggingPlayer.sendMessage("§cYou cannot tag players during the grace period!");
            event.setCancelled(true);
            return;
        }

        PlayerManager taggingData = plugin.playermanager.get(taggingPlayer.getUniqueId());
        PlayerManager taggedData = plugin.playermanager.get(taggedPlayer.getUniqueId());

        if (taggingData == null || taggedData == null) return; // Ensure both players have data
        if (!taggingData.isIngame() || !taggedData.isIngame()) return; // Ensure both are in-game

        // Prevent tagging other zombies
        if (taggedData.isIsdead()) {
            taggingPlayer.sendMessage("§cYou cannot tag another zombie!");
            event.setCancelled(true);
            return;
        }
     // Prevent tagging if tagging player is not a zombie
        if (!taggingData.isIsdead()) { 
            taggingPlayer.sendMessage("§cYou are a survivor and cannot tag players!");
            event.setCancelled(true);
            return;
        }


        // Turn the tagged player into a zombie
        taggedData.setIsdead(true);
        applySuspiciousStewBlindness(taggedPlayer);
        taggedPlayer.sendTitle(
            "§cYou've been Tagged!!", // Title
            "§eTag others till there are no survivors!", // Subtitle
            10, // Fade-in duration (ticks)
            70, // Stay duration (ticks)
            20  // Fade-out duration (ticks)
        );
        stayStillWarnings.remove(taggedPlayer); // Reset warnings for the newly tagged zombie

     // Save the player's current helmet and replace it with the zombie helmet
        plugin.getStatsManager().updatePlayerStat(taggedPlayer.getUniqueId(), "originalHelmet", 
            plugin.getStatsManager().serializeHelmet(taggedPlayer.getInventory().getHelmet()));
        
        String headItemType = plugin.getConfig().getString("HeadItem.Type", "ZOMBIE_HEAD");
        Material headItemMaterial = Material.matchMaterial(headItemType);
        if (headItemMaterial != null) {
            taggedPlayer.getInventory().setHelmet(new ItemStack(headItemMaterial));
        } else {
            Bukkit.getLogger().severe("Invalid HeadItem.Type in config.yml: " + headItemType);
        }

        // Broadcast tag message
        for (Player gamePlayer : plugin.gamePlayers) {
            gamePlayer.sendMessage("§c" + taggedPlayer.getName() + " has been tagged and turned into a zombie!");
            taggedPlayer.sendActionBar("§cYou have been tagged");
        }

        // Update tagging player's stats
        plugin.statsManager.updatePlayerStat(taggingPlayer.getUniqueId(), "tags", 1);

        boolean allZombies = plugin.gamePlayers.stream().allMatch(player -> {
            PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
            return playerData != null && playerData.isIsdead();
        });

        if (allZombies) {
            for (Player gamePlayer : plugin.gamePlayers) {
                gamePlayer.sendMessage("§cAll players have been turned into zombies! The game will now end.");
            }

            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }

            endGame();
        }


        event.setCancelled(true);
    }
    
    public void applyTagEffect(Player player) {
        int blindnessSeconds = plugin.getConfig().getInt("Effects.BlindnessDuration", 10); // Default: 10
        int nightVisionSeconds = plugin.getConfig().getInt("Effects.NightVisionDuration", 10); // Default: 10
        // Convert seconds to ticks
        int blindnessTicks = blindnessSeconds * 20;
        int nightVisionTicks = nightVisionSeconds * 20;

        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, nightVisionTicks, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessTicks, 0));
    }

    private void turnIntoZombie(Player player) {
        PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());

        if (playerData == null || playerData.isIsdead()) {
            Bukkit.getLogger().warning("Player " + player.getName() + " is already a zombie or has no data.");
            return;
        }

        // Mark player as a zombie
        playerData.setIsdead(true);

        // Stop tracking stay-still timer for the player
        lastLocationMap.remove(player);
        
        // Save player's current helmet before replacing it
        saveHelmet(player);

        // Apply blindness effect
        applySuspiciousStewBlindness(player);

        // Notify players
        for (Player gamePlayer : plugin.gamePlayers) {
            gamePlayer.sendMessage("§c" + player.getName() + " has been turned into a zombie!");
        }

        // Replace helmet with the zombie head
        String headItemType = plugin.getConfig().getString("HeadItem.Type", "ZOMBIE_HEAD");
        Material headItemMaterial = Material.matchMaterial(headItemType);
        if (headItemMaterial != null) {
            player.getInventory().setHelmet(new ItemStack(headItemMaterial));
        } else {
            Bukkit.getLogger().severe("Invalid HeadItem.Type in config.yml: " + headItemType);
        }

        // **Updated: Ensure the game ends immediately if all players are zombies**
        boolean allZombies = plugin.gamePlayers.stream()
        	    .filter(p -> plugin.playermanager.containsKey(p.getUniqueId())) // Only check players with active game data
        	    .allMatch(p -> {
        	        PlayerManager data = plugin.playermanager.get(p.getUniqueId());
        	        return data != null && data.isIsdead();
        	    });

        if (allZombies) {
        	for (Player gamePlayer : plugin.gamePlayers) {
        	        gamePlayer.sendMessage("§cAll players have been turned into zombies! The game will now end.");
        	    }

        	    if (countdownTask != null) {
        	        countdownTask.cancel();
        	        countdownTask = null;
        	    }

        	    endGame();
        	}
    }



    /*** EVENT HANDLERS (PREVENTING UNWANTED ACTIONS) ***/
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
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());

        // Prevent breaking blocks for lobby and game players
        if (playerData != null && (playerData.isIngame() || isPlayerInLobby(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());

        // Prevent placing blocks for lobby and game players
        if (playerData != null && (playerData.isIngame() || isPlayerInLobby(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());

        // Prevent interaction for lobby and game players
        if (playerData != null && (playerData.isIngame() || isPlayerInLobby(player))) {
            event.setCancelled(true);
        }
    }

    /*** STAY-STILL TIMER ***/
    private void startStayStillTimer() {
        if (!plugin.getConfig().getBoolean("StayStillTimer.Enabled", true)) {
            return; // Exit if the stay-still feature is disabled
        }

        int stayStillTime = plugin.getConfig().getInt("StayStillTimer.TimeLimit", 30);
        String stayStillMessage = plugin.getConfig().getString(
                "StayStillTimer.Message", "§cYou stayed still for too long and turned into a zombie!");

        Bukkit.getLogger().info("Starting stay-still timer for " + stayStillTime + " seconds.");

        // Cancel any existing task before starting a new one
        if (stayStillTask != null) {
            stayStillTask.cancel();
        }

        stayStillTask = new BukkitRunnable() {
            final Map<Player, Integer> playerTimers = new HashMap<>();

            @Override
            public void run() {
                for (Player player : new ArrayList<>(plugin.gamePlayers)) {
                    PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
                    if (playerData != null && playerData.isIsdead()) {
                        continue;
                    }

                    Location currentLocation = player.getLocation();
                    Location lastLocation = lastLocationMap.get(player);

                    if (lastLocation != null && currentLocation.distanceSquared(lastLocation) < 0.01) {
                        int timeLeft = playerTimers.getOrDefault(player, stayStillTime);

                        if (timeLeft > 0) {
                            playerTimers.put(player, timeLeft - 1);

                            if (timeLeft <= 7) {
                                player.sendMessage("§cMove or you'll turn into a zombie in §e" + timeLeft + " seconds!");
                            }
                        } else {
                            Bukkit.getLogger().info("Player " + player.getName() + " has stayed still for too long!");
                            player.sendMessage(stayStillMessage);
                            turnIntoZombie(player);
                            playerTimers.remove(player);
                        }
                    } else {
                        playerTimers.remove(player);
                    }

                    lastLocationMap.put(player, currentLocation.clone());
                }
            }
        };

        stayStillTask.runTaskTimer(plugin, 0L, 20L); // Schedule with 0L delay and 20 ticks per second
    }

    private void cancelStayStillTimer() {
        if (stayStillTask != null) {
            stayStillTask.cancel();
            stayStillTask = null;
            Bukkit.getLogger().info("Global stay-still timer has been canceled.");
        }

        // Clear tracking maps
        lastLocationMap.clear();
    }

    /*** PLAYER JOIN/LEAVE HANDLING ***/
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Bukkit.getLogger().info("PlayerQuitEvent triggered for: " + player.getName());

        PlayerManager playerData = plugin.playermanager.get(playerUUID);

        if (playerData != null) {
            restoreHelmet(player);

            if (player.equals(initialZombie)) {
            	for (Player gamePlayer : plugin.gamePlayers) {
                gamePlayer.sendMessage("§cThe initial zombie (" + player.getName() + ") has left the game. Ending the game.");
                endGameAndTeleportAll();
                return;
            	}
            }

            plugin.gamePlayers.remove(player);
            plugin.playermanager.remove(playerUUID);

            if (plugin.gamePlayers.size() < getPlayerNeeded()) {
            	for (Player gamePlayer : plugin.gamePlayers) {
                gamePlayer.sendMessage("§cNot enough players! The game will now reset.");
                
            	}
            	endGameAndTeleportAll();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Bukkit.getLogger().info("PlayerJoinEvent triggered for: " + player.getName());

        // Check if the player was part of the gamePlayers list
        if (plugin.gamePlayers.contains(player)) {
            PlayerManager playerData = plugin.playermanager.get(playerUUID);

            if (playerData != null) {
                Bukkit.getLogger().info("Restoring state for rejoining player: " + player.getName());

                // Restore helmet if applicable
                restoreHelmet(player);

                // Reset player state
                playerData.setIngame(false);
                playerData.setIsdead(false);

                // Remove the player from game and lobby lists
                plugin.gamePlayers.remove(player);
                lobbyPlayers.remove(player);

                // Remove player from playermanager to allow clean rejoin
                plugin.playermanager.remove(playerUUID);

                // Teleport player to the lobby
                teleportToLobby(player);
                Bukkit.getLogger().info("Cleared game data and teleported rejoining player: " + player.getName());
            } else {
                Bukkit.getLogger().warning("Player data missing for game player: " + player.getName());
            }
        } else {
            // Log and ignore players who were not part of the game
            Bukkit.getLogger().info("Player " + player.getName() + " was not in a game. Skipping teleport.");
        }
    }

    private void teleportToLobby(Player player) {
        String worldName = plugin.getConfig().getString("LobbySpawn.world");
        double x = plugin.getConfig().getDouble("LobbySpawn.X");
        double y = plugin.getConfig().getDouble("LobbySpawn.Y");
        double z = plugin.getConfig().getDouble("LobbySpawn.Z");
        float yaw = (float) plugin.getConfig().getDouble("LobbySpawn.Yaw", 0); // Default yaw 0
        float pitch = (float) plugin.getConfig().getDouble("LobbySpawn.Pitch", 0); // Default pitch 0

        World world = (worldName != null && !worldName.isEmpty()) ? plugin.getServer().getWorld(worldName) : null;
        if (world != null) {
            Location lobbySpawn = new Location(world, x, y, z, yaw, pitch);
            player.teleport(lobbySpawn);
            player.sendMessage("§aYou have been returned to the lobby.");
            Bukkit.getLogger().info("Teleported player " + player.getName() + " to the lobby.");
        } else {
            Bukkit.getLogger().severe("Lobby spawn world is invalid or not set: " + worldName);
            player.sendMessage("§cThe lobby spawn location is invalid. Please contact an admin.");
        }
    }

    /*** HELMET MANAGEMENT ***/
    private void saveHelmet(Player player) {
        ItemStack currentHelmet = player.getInventory().getHelmet();
        if (currentHelmet != null) {
            // Serialize and save the helmet to the stats file via StatsManager
            String serializedHelmet = plugin.getStatsManager().serializeHelmet(currentHelmet);
            plugin.getStatsManager().updatePlayerStat(player.getUniqueId(), "originalHelmet", serializedHelmet);
            Bukkit.getLogger().info("Saved helmet for player: " + player.getName() + " - " + currentHelmet.getType());
        } else {
            // Save "none" if no helmet exists
            plugin.getStatsManager().updatePlayerStat(player.getUniqueId(), "originalHelmet", "none");
            Bukkit.getLogger().info("No helmet to save for player: " + player.getName());
        }

        // Replace the helmet with the zombie head
        player.getInventory().setHelmet(new ItemStack(Material.matchMaterial(
                plugin.getConfig().getString("HeadItem.Type", "ZOMBIE_HEAD"))));
    }

    private void restoreHelmet(Player player) {
        // Retrieve the serialized helmet from the stats file
        String serializedHelmet = plugin.getStatsManager().getPlayerStat(player.getUniqueId(), "originalHelmet", "none");

        if (!serializedHelmet.equals("none")) {
            // Deserialize the helmet via StatsManager and apply it to the player
            ItemStack originalHelmet = plugin.getStatsManager().deserializeHelmet(serializedHelmet);
            if (originalHelmet != null) {
                player.getInventory().setHelmet(originalHelmet);
                Bukkit.getLogger().info("Restored helmet for player: " + player.getName() + " - " + originalHelmet.getType());
            } else {
                Bukkit.getLogger().severe("Failed to restore helmet for player: " + player.getName() + ". Deserialized helmet was null.");
            }
        } else {
            // Clear the helmet slot if no original helmet was saved
            player.getInventory().setHelmet(null);
            Bukkit.getLogger().info("Cleared helmet for player: " + player.getName());
        }

        // Clear the saved helmet data
        plugin.getStatsManager().updatePlayerStat(player.getUniqueId(), "originalHelmet", "none");
    }
}

