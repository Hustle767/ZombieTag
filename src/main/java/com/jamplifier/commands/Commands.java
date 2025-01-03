package com.jamplifier.commands;

import com.jamplifier.MainClass;
import com.jamplifier.PlayerData.PlayerManager;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    private MainClass plugin;

    public Commands(MainClass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Show help if the player has the 'zombietag.player' permission
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (player.hasPermission("zombietag.player")) {
                showHelp(player);
            } else {
                player.sendMessage("§cYou do not have permission to access help.");
            }
            return true;
        }
     // Lobby Join command
        if (!player.hasPermission("zombietag.player")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        // Handle 'join' command
        if (args.length == 1 && args[0].equalsIgnoreCase("join")) {
            return handleJoin(player);
        }
        // handle leave command
        if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
            return handleLeave(player);
        }

        // Handle commands with arguments
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("teleport")) {
                // Admin commands need 'zombietag.admin' permission
                if (!player.hasPermission("zombietag.admin")) {
                    player.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }

                // Handle 'setspawn' and 'teleport' commands
                if (args[0].equalsIgnoreCase("setspawn")) {
                    return handleSetSpawn(player, args[1]);
                } else if (args[0].equalsIgnoreCase("teleport")) {
                    return handleTeleport(player, args[1]);
                }
            }
        }

        // If command does not match, show usage
        player.sendMessage("§eUsage: /zombietag <help|setspawn|teleport> <lobby|game>");
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§eZombieTag Commands:");
        player.sendMessage("  §7/zombietag setspawn <lobby|game> - Set the spawn location for the lobby or game.");
        player.sendMessage("  §7/zombietag teleport <lobby|game> - Teleport to the lobby or game spawn.");
        player.sendMessage("  §7/zombietag help - Show this help message.");
    }

    private boolean handleSetSpawn(Player player, String type) {
        Location loc = player.getLocation();
        if (type.equalsIgnoreCase("lobby")) {
            plugin.getConfig().set("LobbySpawn.world", loc.getWorld().getName());
            plugin.getConfig().set("LobbySpawn.X", loc.getX());
            plugin.getConfig().set("LobbySpawn.Y", loc.getY());
            plugin.getConfig().set("LobbySpawn.Z", loc.getZ());
            plugin.saveConfig();
            player.sendMessage("§aLobby spawn set!");
        } else if (type.equalsIgnoreCase("game")) {
            plugin.getConfig().set("GameSpawn.world", loc.getWorld().getName());
            plugin.getConfig().set("GameSpawn.X", loc.getX());
            plugin.getConfig().set("GameSpawn.Y", loc.getY());
            plugin.getConfig().set("GameSpawn.Z", loc.getZ());
            plugin.saveConfig();
            player.sendMessage("§aGame spawn set!");
        } else {
            player.sendMessage("§cInvalid spawn type! Use 'lobby' or 'game'.");
            return false;
        }
        return true;
    }

    private boolean handleTeleport(Player player, String type) {
        if (!player.hasPermission("zombietag.admin.teleport")) {
            player.sendMessage("§cYou do not have permission to teleport!");
            return false;
        }

        if (type.equalsIgnoreCase("lobby")) {
            double x = plugin.getConfig().getDouble("LobbySpawn.X");
            double y = plugin.getConfig().getDouble("LobbySpawn.Y");
            double z = plugin.getConfig().getDouble("LobbySpawn.Z");
            String worldName = plugin.getConfig().getString("LobbySpawn.world");
            if (worldName != null && !worldName.isEmpty()) {
                player.teleport(new Location(plugin.getServer().getWorld(worldName), x, y, z));
                player.sendMessage("§aTeleported to the lobby spawn.");
            } else {
                player.sendMessage("§cLobby spawn is not set.");
            }
        } else if (type.equalsIgnoreCase("game")) {
            double x = plugin.getConfig().getDouble("GameSpawn.X");
            double y = plugin.getConfig().getDouble("GameSpawn.Y");
            double z = plugin.getConfig().getDouble("GameSpawn.Z");
            String worldName = plugin.getConfig().getString("GameSpawn.world");
            if (worldName != null && !worldName.isEmpty()) {
                player.teleport(new Location(plugin.getServer().getWorld(worldName), x, y, z));
                player.sendMessage("§aTeleported to the game spawn.");
            } else {
                player.sendMessage("§cGame spawn is not set.");
            }
        } else {
            player.sendMessage("§cInvalid teleport type! Use 'lobby' or 'game'.");
           
            
            
        }
		return false;
    }
        // Handles /zombietag join
    private boolean handleJoin(Player player) {
        // Check if the player is already in a game
        if (plugin.playermanager.containsKey(player.getUniqueId())) {
            PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
            if (playerData.isIngame()) {
                player.sendMessage("§cYou are already in a game!");
                return true;
            }
        }

        // Check if the player is already in the lobby
        if (plugin.playermanager.containsKey(player.getUniqueId())) {
            PlayerManager playerData = plugin.playermanager.get(player.getUniqueId());
            if (!playerData.isIngame() && !playerData.isIsdead()) {
                player.sendMessage("§cYou are already in the lobby!");
                return true;
            }
        }

        // Check if a game is in progress
        if (plugin.gameManager.isGameInProgress()) {
            player.sendMessage("§cA game is already in progress. Please wait for the next round.");
            return true;
        }

        // Check if the lobby has reached max players
        int currentPlayers = plugin.playermanager.size();
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default to 20 if not set

        if (currentPlayers >= maxPlayers) {
            player.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return true;
        }

        // Remove any old player data (if needed) and add them to the lobby
        PlayerManager playerData = new PlayerManager(player.getUniqueId(), false, false); // false: not in game, not dead
        plugin.playermanager.put(player.getUniqueId(), playerData); // Add to playermanager (lobby)

        // Teleport player to the lobby spawn
        double x = plugin.getConfig().getDouble("LobbySpawn.X");
        double y = plugin.getConfig().getDouble("LobbySpawn.Y");
        double z = plugin.getConfig().getDouble("LobbySpawn.Z");
        String worldName = plugin.getConfig().getString("LobbySpawn.world");

        if (worldName != null && !worldName.isEmpty()) {
            player.teleport(new Location(plugin.getServer().getWorld(worldName), x, y, z));
            player.sendMessage("§aYou have been teleported to the lobby!");

            // Call the lobbyWait function to handle the countdown logic
            plugin.gameManager.lobbyWait(player); // Ensure this method is called to handle the countdown logic.
        } else {
            player.sendMessage("§cLobby spawn is not set.");
        }

        return true;
    }




    private boolean handleLeave(Player player) {
        UUID playerId = player.getUniqueId();

        if (!plugin.playermanager.containsKey(playerId)) {
            player.sendMessage("§cYou are not currently in the lobby or game!");
            return true;
        }

        PlayerManager playerData = plugin.playermanager.get(playerId);

        if (playerData.isIngame()) {
            player.sendMessage("§cYou are currently in a game! You must finish the game before leaving.");
            return true;
        }

        // Remove the player from the lobby
        plugin.playermanager.remove(playerId);
        plugin.getGameManager().removePlayerFromLobby(player);

        // Check if the countdown needs to be canceled
        if (plugin.getGameManager().lobbyPlayers.size() < plugin.getConfig().getInt("PlayerNeeded", 2)) {
            plugin.getGameManager().cancelCountdown();
        }

        player.sendMessage("§aYou have successfully left the lobby.");
        return true;
    }
}