package com.jamplifier.commands;

import com.jamplifier.MainClass;
import com.jamplifier.PlayerData.PlayerManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (player.hasPermission("zombietag.admin")) {
                showHelp(player);
            } else {
                showHelpAdmin(player);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Admin-only commands
        if (subCommand.equals("reload")) {
            Bukkit.getLogger().info(player.getName() + " is trying to execute reload.");
            if (player.hasPermission("zombietag.admin")) {
                return handleReload(player);
            } else {
                player.sendMessage("§cYou do not have permission to use this command!");
                return true;
            }
        }

        if (subCommand.equals("setspawn")) {
            if (player.hasPermission("zombietag.admin")) {
                if (args.length == 2) {
                    return handleSetSpawn(player, args[1]);
                } else {
                    player.sendMessage("§eUsage: /zombietag setspawn <lobby|game>");
                }
            } else {
                player.sendMessage("§cYou do not have permission to use this command!");
            }
            return true;
        }

        if (subCommand.equals("teleport")) {
            if (player.hasPermission("zombietag.admin")) {
                if (args.length == 2) {
                    return handleTeleport(player, args[1]);
                } else {
                    player.sendMessage("§eUsage: /zombietag teleport <lobby|game>");
                }
            } else {
                player.sendMessage("§cYou do not have permission to use this command!");
            }
            return true;
        }

        // Player commands
        if (subCommand.equals("join")) {
            if (player.hasPermission("zombietag.player")) {
                return handleJoin(player);
            } else {
                player.sendMessage("§cYou do not have permission to use this command!");
            }
            return true;
        }

        if (subCommand.equals("leave")) {
            if (player.hasPermission("zombietag.player")) {
                return handleLeave(player);
            } else {
                player.sendMessage("§cYou do not have permission to use this command!");
            }
            return true;
        }
        if (subCommand.equals("top")) {
            if (player.hasPermission("zombietag.player")) {
                player.sendMessage("§6--- Zombie Tag Leaderboard ---");

                // Top 5 Taggers
                List<Map.Entry<UUID, Integer>> taggersLeaderboard = plugin.getStatsManager().getLeaderboard("tags");
                player.sendMessage("§cTop 5 Taggers:");
                if (taggersLeaderboard.isEmpty()) {
                    player.sendMessage("§7No data available.");
                } else {
                    for (int i = 0; i < Math.min(5, taggersLeaderboard.size()); i++) {
                        Map.Entry<UUID, Integer> entry = taggersLeaderboard.get(i);
                        String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                        int statValue = entry.getValue();
                        player.sendMessage("§e" + (i + 1) + ". §a" + playerName + ": §b" + statValue);
                    }
                }

                // Top 5 Survivors
                List<Map.Entry<UUID, Integer>> survivorsLeaderboard = plugin.getStatsManager().getLeaderboard("survivals");
                player.sendMessage("§cTop 5 Survivors:");
                if (survivorsLeaderboard.isEmpty()) {
                    player.sendMessage("§7No data available.");
                } else {
                    for (int i = 0; i < Math.min(5, survivorsLeaderboard.size()); i++) {
                        Map.Entry<UUID, Integer> entry = survivorsLeaderboard.get(i);
                        String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                        int statValue = entry.getValue();
                        player.sendMessage("§e" + (i + 1) + ". §a" + playerName + ": §b" + statValue);
                    }
                }

                player.sendMessage("§6-----------------------------");
            } else {
                player.sendMessage("§cYou do not have permission to use this command!");
            }
            return true;
        }
        if (subCommand.equalsIgnoreCase("stats")) {
            if (player.hasPermission("zombietag.player")) {
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage("§cPlayer not found!");
                        return true;
                    }

                    UUID targetUUID = target.getUniqueId();
                    int tags = Integer.parseInt(plugin.getStatsManager().getPlayerStat(targetUUID, "tags", "0"));
                    int survivals = Integer.parseInt(plugin.getStatsManager().getPlayerStat(targetUUID, "survivals", "0"));

                    player.sendMessage("§6--- Zombie Tag Stats for " + target.getName() + " ---");
                    player.sendMessage("§aTags: §e" + tags);
                    player.sendMessage("§aSurvivals: §e" + survivals);
                    player.sendMessage("§6--------------------------------");
                } else {
                    // Show the stats of the command issuer
                    UUID playerUUID = player.getUniqueId();
                    int tags = Integer.parseInt(plugin.getStatsManager().getPlayerStat(playerUUID, "tags", "0"));
                    int survivals = Integer.parseInt(plugin.getStatsManager().getPlayerStat(playerUUID, "survivals", "0"));

                    player.sendMessage("§6--- Your Zombie Tag Stats ---");
                    player.sendMessage("§aTags: §e" + tags);
                    player.sendMessage("§aSurvivals: §e" + survivals);
                    player.sendMessage("§6-------------------------------");
                }
            } else {
                player.sendMessage("§cYou do not have permission to use this command!");
            }
            return true;
        }




        // Invalid command
        player.sendMessage("§eInvalid command. Use /zombietag help for available commands.");
        return true;
    }



    private void showHelp(Player player) {
        player.sendMessage("§eZombieTag Commands:");
        player.sendMessage("  §7/zombietag <join|leave>");
        player.sendMessage("  §7/zombietag help - Show this help message.");
        player.sendMessage("  §7/zombietag top - View top players");
        player.sendMessage("  §7/zombietag stats - View your statistics");
    }
    private void showHelpAdmin(Player player) {
        player.sendMessage("§eZombieTag Commands:");
        player.sendMessage("  §7/zombietag <join|leave>");
        player.sendMessage("  §7/zombietag setspawn <lobby|game> - Set the spawn location for the lobby or game.");
        player.sendMessage("  §7/zombietag teleport <lobby|game> - Teleport to the lobby or game spawn.");
        player.sendMessage("  §7/zombietag help - Show this help message.");
    }

    private boolean handleSetSpawn(Player player, String type) {
        if (!player.hasPermission("zombietag.admin")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return false;
        }

        Location loc = player.getLocation();
        String path = type.equalsIgnoreCase("lobby") ? "LobbySpawn" : "GameSpawn";

        if (!type.equalsIgnoreCase("lobby") && !type.equalsIgnoreCase("game")) {
            player.sendMessage("§cInvalid spawn type! Use 'lobby' or 'game'.");
            return false;
        }

        // Save world, coordinates, yaw (left/right), and pitch (up/down)
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".X", loc.getX());
        plugin.getConfig().set(path + ".Y", loc.getY());
        plugin.getConfig().set(path + ".Z", loc.getZ());
        plugin.getConfig().set(path + ".Yaw", loc.getYaw()); // Facing direction (left/right)
        plugin.getConfig().set(path + ".Pitch", loc.getPitch()); // Looking up/down
        plugin.saveConfig();

        player.sendMessage("§a" + type.substring(0, 1).toUpperCase() + type.substring(1) + " spawn set!");
        return true;
    }


    private boolean handleTeleport(Player player, String type) {
        if (!player.hasPermission("zombietag.admin")) {
            player.sendMessage("§cYou do not have permission to teleport!");
            return false;
        }

        if (type.equalsIgnoreCase("lobby")) {
            double x = plugin.getConfig().getDouble("LobbySpawn.X");
            double y = plugin.getConfig().getDouble("LobbySpawn.Y");
            double z = plugin.getConfig().getDouble("LobbySpawn.Z");
            float yaw = (float) plugin.getConfig().getDouble("LobbySpawn.Yaw", 0); // Default yaw 0
            float pitch = (float) plugin.getConfig().getDouble("LobbySpawn.Pitch", 0); // Default pitch 0
            String worldName = plugin.getConfig().getString("LobbySpawn.world");
            if (worldName != null && !worldName.isEmpty()) {
                player.teleport(new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch));
                player.sendMessage("§aTeleported to the lobby spawn.");
            } else {
                player.sendMessage("§cLobby spawn is not set.");
            }
        } else if (type.equalsIgnoreCase("game")) {
            double x = plugin.getConfig().getDouble("GameSpawn.X");
            double y = plugin.getConfig().getDouble("GameSpawn.Y");
            double z = plugin.getConfig().getDouble("GameSpawn.Z");
            float yaw = (float) plugin.getConfig().getDouble("GameSpawn.Yaw", 0); // Default yaw 0
            float pitch = (float) plugin.getConfig().getDouble("GameSpawn.Pitch", 0); // Default pitch 0
            String worldName = plugin.getConfig().getString("GameSpawn.world");
            if (worldName != null && !worldName.isEmpty()) {
                player.teleport(new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch));
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
        UUID playerUUID = player.getUniqueId();

        // Check if the player is already in a game
        if (plugin.playermanager.containsKey(playerUUID)) {
            PlayerManager playerData = plugin.playermanager.get(playerUUID);
            if (playerData.isIngame()) {
                player.sendMessage("§cYou are already in a game!");
                return true;
            }
        }

        if (plugin.gameManager.getLobbyPlayers().contains(player) && plugin.playermanager.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou are already in the lobby queue!");
            return true;
        }


        // If the game is in progress, queue the player for the next round
        if (plugin.gameManager.isGameInProgress()) {
            player.sendMessage("§eA game is already in progress. You will be placed in the lobby for the next round.");

            // Check if the player is already stored in the player manager
            if (!plugin.playermanager.containsKey(playerUUID)) {
                PlayerManager playerData = new PlayerManager(playerUUID, false, false); // Not in game, not dead
                plugin.playermanager.put(playerUUID, playerData);
            }

            // Teleport player to the lobby spawn
            teleportToLobby(player);

            // Add them to the lobby queue **only if they're not already in it**
            if (!plugin.gameManager.getLobbyPlayers().contains(player)) {
                plugin.gameManager.getLobbyPlayers().add(player);
            }

            return true;
        }

        // Check if the lobby has reached max players
        int currentPlayers = plugin.gameManager.getLobbyPlayers().size();
        int maxPlayers = plugin.getConfig().getInt("MaxPlayers", 20); // Default to 20 if not set

        if (currentPlayers >= maxPlayers) {
            player.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return true;
        }

        // Add the player to the player manager if they're not already stored
        if (!plugin.playermanager.containsKey(playerUUID)) {
            PlayerManager playerData = new PlayerManager(playerUUID, false, false); // Not in game, not dead
            plugin.playermanager.put(playerUUID, playerData);
        }

        // Teleport the player to the lobby spawn
        teleportToLobby(player);

        // Call lobbyWait function to start the countdown logic
        plugin.gameManager.lobbyWait(player);

        return true;
    }
    private void teleportToLobby(Player player) {
        double x = plugin.getConfig().getDouble("LobbySpawn.X");
        double y = plugin.getConfig().getDouble("LobbySpawn.Y");
        double z = plugin.getConfig().getDouble("LobbySpawn.Z");
        float yaw = (float) plugin.getConfig().getDouble("LobbySpawn.Yaw"); // Retrieve yaw
        float pitch = (float) plugin.getConfig().getDouble("LobbySpawn.Pitch"); // Retrieve pitch
        String worldName = plugin.getConfig().getString("LobbySpawn.world");

        if (worldName != null && !worldName.isEmpty()) {
            player.teleport(new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch));
            player.sendMessage("§aYou have been teleported to the lobby!");
        } else {
            player.sendMessage("§cLobby spawn is not set.");
        }
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
    private boolean handleReload(Player player) {
        if (!player.hasPermission("zombietag.admin")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        plugin.reloadConfig();
        plugin.getGameManager().setupGame(); // Reload spawn locations and other settings
        player.sendMessage("§aZombieTag configuration reloaded!");
        return true;
    }

}