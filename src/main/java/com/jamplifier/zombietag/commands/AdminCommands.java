package com.jamplifier.zombietag.commands;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.GameService;
import com.jamplifier.zombietag.core.LobbyService;
import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.stats.ConfigStats;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class AdminCommands {

    private final MainClass plugin;
    private final LobbyService lobby;
    private final GameService game;
    private final ConfigStats stats;
    private final Settings settings;
    private final Spawns spawns;

    @SuppressWarnings("unused") // kept for parity with constructor call, not used here
    private final PlayerRegistry registry;

    public AdminCommands(MainClass plugin,
                         LobbyService lobby,
                         GameService game,
                         ConfigStats stats,
                         Settings settings,
                         Spawns spawns,
                         PlayerRegistry registry) {
        this.plugin = plugin;
        this.lobby = lobby;
        this.game = game;
        this.stats = stats;
        this.settings = settings;
        this.spawns = spawns;
        this.registry = registry;
    }

    public boolean reload(Player p) {
        if (!p.hasPermission("zombietag.admin")) { p.sendMessage("§cNo permission."); return true; }

        // Full rebuild of services/state
        plugin.reloadAll();

        // Rebind the command executor so it points at the fresh instances
        if (plugin.getCommand("zombietag") != null) {
            plugin.getCommand("zombietag").setExecutor(
                new CommandsRouter(
                    plugin,
                    plugin.getLobbyService(),
                    plugin.getGameService(),
                    plugin.getStats(),
                    plugin.getSettings(),
                    plugin.getSpawns(),
                    plugin.getRegistry()
                )
            );
        }

        // Note: We are NOT re-registering listeners here to avoid double-registration.
        // Listeners will still reference old instances until next server restart.
        // If you want a full hot-reload, add an explicit unregister/re-register flow.

        p.sendMessage("§aZombieTag configuration reloaded!");
        return true;
    }

    public boolean setspawn(Player p, String[] args) {
        if (!p.hasPermission("zombietag.admin")) { p.sendMessage("§cNo permission."); return true; }
        if (args.length != 2) { p.sendMessage("§eUsage: /zombietag setspawn <lobby|game>"); return true; }

        String type = args[1].toLowerCase();
        if (!type.equals("lobby") && !type.equals("game")) {
            p.sendMessage("§cInvalid type. Use lobby|game");
            return true;
        }

        // Save location into config
        Location loc = p.getLocation();
        String path = type.equals("lobby") ? "LobbySpawn" : "GameSpawn";
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".X", loc.getX());
        plugin.getConfig().set(path + ".Y", loc.getY());
        plugin.getConfig().set(path + ".Z", loc.getZ());
        plugin.getConfig().set(path + ".Yaw", loc.getYaw());
        plugin.getConfig().set(path + ".Pitch", loc.getPitch());
        plugin.saveConfig();
        plugin.getSpawns().reload(plugin.getConfig());

        p.sendMessage("§a" + Character.toUpperCase(type.charAt(0)) + type.substring(1) + " spawn set!");
        return true;
    }

    public boolean teleport(Player p, String[] args) {
        if (!p.hasPermission("zombietag.admin")) { p.sendMessage("§cNo permission."); return true; }
        if (args.length != 2) { p.sendMessage("§eUsage: /zombietag teleport <lobby|game>"); return true; }

        String which = args[1].toLowerCase();
        switch (which) {
            case "lobby": {
                Location ls = readSpawnFromConfig("LobbySpawn");
                if (ls == null) { p.sendMessage("§cLobby spawn not set or world missing."); return true; }
                p.teleport(ls);
                p.sendMessage("§aTeleported to lobby spawn.");
                return true;
            }
            case "game": {
                Location gs = readSpawnFromConfig("GameSpawn");
                if (gs == null) { p.sendMessage("§cGame spawn not set or world missing."); return true; }
                p.teleport(gs);
                p.sendMessage("§aTeleported to game spawn.");
                return true;
            }
            default:
                p.sendMessage("§cInvalid teleport type! Use 'lobby' or 'game'.");
                return true;
        }
    }

    // ---- helpers ----

    private Location readSpawnFromConfig(String basePath) {
        FileConfiguration cfg = plugin.getConfig();
        String worldName = cfg.getString(basePath + ".world", null);
        if (worldName == null || worldName.isEmpty()) return null;

        World w = plugin.getServer().getWorld(worldName);
        if (w == null) return null;

        double x = cfg.getDouble(basePath + ".X");
        double y = cfg.getDouble(basePath + ".Y");
        double z = cfg.getDouble(basePath + ".Z");
        float yaw = (float) cfg.getDouble(basePath + ".Yaw", 0.0);
        float pitch = (float) cfg.getDouble(basePath + ".Pitch", 0.0);

        return new Location(w, x, y, z, yaw, pitch);
    }
}
