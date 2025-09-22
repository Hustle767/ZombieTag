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

        // 1) Unregister all existing listeners for this plugin
        org.bukkit.event.HandlerList.unregisterAll(plugin);

        // 2) Rebuild config & services (creates fresh Settings/GameState/Registry/etc.)
        plugin.reloadAll();  // make sure this cancels old timers via gameState.clearAll()

        // 3) Re-register listeners against the fresh instances
        plugin.registerListeners();

        // 4) Rebind command router + tab-completer to fresh instances
        var root = plugin.getCommand("zombietag");
        if (root != null) {
            var router = new CommandsRouter(
                plugin,
                plugin.getLobbyService(),
                plugin.getGameService(),
                plugin.getStats(),
                plugin.getSettings(),
                plugin.getSpawns(),
                plugin.getRegistry()
            );
            root.setExecutor(router);
            root.setTabCompleter(new com.jamplifier.zombietag.Util.CommandsTabCompleter(plugin));
        }

        p.sendMessage("§aZombieTag reloaded (listeners & commands rebound).");
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
    public boolean info(Player p, String[] args) {
        if (!p.hasPermission("zombietag.admin")) { p.sendMessage("§cNo permission."); return true; }

        String section = (args.length >= 2) ? args[1].toLowerCase() : "all";

        // helpers
        java.util.function.Function<Boolean,String> yn = b -> b ? "§aON" : "§cOFF";
        java.util.function.Function<Integer,String> secs = v -> v + "s";
        java.util.function.Function<org.bukkit.Location,String> fmtLoc = loc -> {
            if (loc == null || loc.getWorld() == null) return "§7not set";
            return String.format("§f%s§7: §f%.1f§7, §f%.1f§7, §f%.1f §8(yaw %.1f, pitch %.1f)",
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        };

        // pull from your Settings (matches Settings.java exactly)
        int     playerNeeded    = settings.playerNeeded;
        int     maxPlayers      = settings.maxPlayers;
        int     lobbyCountdown  = settings.lobbyCountdownSeconds;
        boolean autoRejoin      = settings.autoRejoin;

        int     gameLen         = settings.gameLengthSeconds;
        int     graceSeconds    = settings.graceSeconds;
        boolean announceLen     = settings.announceGameLength;

        boolean rewardEnabled   = settings.rewardEnabled;
        String  rewardCmd       = settings.rewardCommand;

        String  helmetItem      = settings.headItemType;

        int     blindSec        = settings.blindnessSeconds;
        int     nightVisSec     = settings.nightVisionSeconds;

        boolean stayStillOn     = settings.stayStillEnabled;
        int     stayStillSec    = settings.stayStillSeconds;
        String  stayStillMsg    = settings.stayStillMessage;

        org.bukkit.Location lobbySpawn = spawns.lobby();
        org.bukkit.Location gameSpawn  = spawns.game();

        p.sendMessage("§8§m--------------------§r §aZombieTag §7Info §8§m--------------------");

        // LOBBY
        if (section.equals("all") || section.equals("lobby")) {
            p.sendMessage("§bLobby");
            p.sendMessage("  §7Needed: §f" + playerNeeded + "  §7Max: §f" + maxPlayers);
            p.sendMessage("  §7Countdown: §f" + secs.apply(lobbyCountdown));
            p.sendMessage("  §7Auto-rejoin: " + yn.apply(autoRejoin));
            p.sendMessage("  §7Queue during game: " + yn.apply(settings.queueDuringGame));
        }

        // GAME
        if (section.equals("all") || section.equals("game")) {
            p.sendMessage("§bGame");
            p.sendMessage("  §7Length: §f" + secs.apply(gameLen) + "  §7Grace: §f" + secs.apply(graceSeconds));
            p.sendMessage("  §7Announce length: " + yn.apply(announceLen));
        }

        // REWARDS
        if (section.equals("all") || section.equals("rewards")) {
            p.sendMessage("§bRewards (Survivors)");
            p.sendMessage("  §7Enabled: " + yn.apply(rewardEnabled));
            p.sendMessage("  §7Command: §f" + (rewardCmd == null ? "§7none" : rewardCmd));
        }

        // ITEMS
        if (section.equals("all") || section.equals("items")) {
            p.sendMessage("§bItems");
            p.sendMessage("  §7Zombie helmet: §f" + (helmetItem == null ? "§7default" : helmetItem));
        }

        // EFFECTS
        if (section.equals("all") || section.equals("effects")) {
            p.sendMessage("§bEffects at Start");
            p.sendMessage("  §7Blindness: §f" + secs.apply(blindSec) + "  §7Night Vision: §f" + secs.apply(nightVisSec));
        }

        // STAY STILL
        if (section.equals("all") || section.equals("stay") || section.equals("staystill")) {
            p.sendMessage("§bStay-Still");
            p.sendMessage("  §7Enabled: " + yn.apply(stayStillOn) + "  §7Time limit: §f" + secs.apply(stayStillSec));
            p.sendMessage("  §7Message: §f" + (stayStillMsg == null ? "§7default" : stayStillMsg));
        }

        // SPAWNS
        if (section.equals("all") || section.equals("spawns") || section.equals("spawn")) {
            p.sendMessage("§bSpawns");
            p.sendMessage("  §7Lobby: " + fmtLoc.apply(lobbySpawn));
            p.sendMessage("  §7Game:  " + fmtLoc.apply(gameSpawn));
        }

        p.sendMessage("§8§m---------------------------------------------------------");
        return true;
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
