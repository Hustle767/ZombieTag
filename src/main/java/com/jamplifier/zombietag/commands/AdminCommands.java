package com.jamplifier.zombietag.commands;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.GamePhase;
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

    @SuppressWarnings("unused")
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
        if (!p.hasPermission("zombietag.admin")) {
            p.sendMessage("§cNo permission.");
            return true;
        }

        // 1. Safely clean up current round / lobby BEFORE tearing down services.
        safeCleanupBeforeReload(p);

        // 2. Unregister all listeners from the *old* plugin state.
        org.bukkit.event.HandlerList.unregisterAll(plugin);

        // 3. Rebuild config, services, listeners, commands.
        plugin.reloadAll();
        plugin.registerListeners();

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

        p.sendMessage("§aZombieTag reloaded: game state reset, listeners rebound.");
        return true;
    }

    /**
     * Gracefully dump everyone out of the current game/lobby
     * so we don't leave dangling Player refs or timers
     * when we rebuild GameState/Services.
     */
    private void safeCleanupBeforeReload(Player executor) {
        var state = plugin.getGameState();

        // A. If a game is currently running, end it cleanly using existing logic.
        if (state.isRunning() || state.getPhase() == GamePhase.ENDING) {
            executor.sendMessage("§7[ZombieTag] §eEnding active round before reload...");
            // endGame(false) handles:
            // - rewards/announce
            // - teleport to lobby spawn
            // - restoring helmets
            // - clearing timers
            // - setting phase back to LOBBY
            plugin.getGameService().endGame(false);
        }

        // B. Now kick anyone still in the lobby queue out to the exit spawn
        //    and let them know what's going on.
        var lobbyPlayers = new java.util.ArrayList<>(state.getLobbyPlayers());
        if (!lobbyPlayers.isEmpty()) {
            executor.sendMessage("§7[ZombieTag] §eClearing lobby queue (" + lobbyPlayers.size() + " players)...");
        }

        for (org.bukkit.entity.Player lp : lobbyPlayers) {
            if (lp == null || !lp.isOnline()) continue;

            // tell them first so it doesn't look like a random teleport
            lp.sendMessage("§eZombieTag reloading. You have been removed from the queue.");

            // teleport them to exit spawn (or fallback spawns) similar to PlayerCommands.teleportToExit()
            org.bukkit.Location es = plugin.getSpawns().exit();
            if (es != null) {
                lp.teleport(es);
            } else {
                org.bukkit.Location ls = plugin.getSpawns().lobby();
                if (ls != null && ls.getWorld() != null) {
                    lp.teleport(ls.getWorld().getSpawnLocation());
                } else {
                    lp.teleport(lp.getWorld().getSpawnLocation());
                }
            }
        }

        // C. Wipe lobby/game lists + flags in registry so nobody is still "ingame"
        // We'll mark everyone not ingame and not zombie to avoid stuck state.
        for (java.util.UUID id : new java.util.ArrayList<>(plugin.getRegistry().all()
                .stream()
                .map(ps -> ps.getUuid())
                .toList())) {
            var ps = plugin.getRegistry().get(id);
            if (ps != null) {
                ps.setIngame(false);
                ps.setZombie(false);
            }
        }

        state.getLobbyPlayers().clear();
        state.getGamePlayers().clear();
        state.setInitialZombie(null);

        // Kill any countdown / timers just in case
        if (state.getLobbyCountdownTask() != null) {
            try { state.getLobbyCountdownTask().cancel(); } catch (Throwable ignored) {}
            state.setLobbyCountdownTask(null);
        }
        if (state.getGameTimerTask() != null) {
            try { state.getGameTimerTask().cancel(); } catch (Throwable ignored) {}
            state.setGameTimerTask(null);
        }
        if (state.getStayStillTask() != null) {
            try { state.getStayStillTask().cancel(); } catch (Throwable ignored) {}
            state.setStayStillTask(null);
        }

        // Back to lobby phase so new GameState after reload starts neutral.
        state.setPhase(GamePhase.LOBBY);
    }


    public boolean setspawn(Player p, String[] args) {
        if (!p.hasPermission("zombietag.admin")) { p.sendMessage("§cNo permission."); return true; }
        if (args.length != 2) { p.sendMessage("§eUsage: /zombietag setspawn <lobby|game|exit>"); return true; }

        String type = args[1].toLowerCase();
        if (!type.equals("lobby") && !type.equals("game") && !type.equals("exit")) {
            p.sendMessage("§cInvalid type. Use lobby|game|exit");
            return true;
        }

        var cfg = plugin.getConfig();
        var loc = p.getLocation();
        String base = "spawns." + type;
        cfg.set(base + ".world", loc.getWorld().getName());
        cfg.set(base + ".x", loc.getX());
        cfg.set(base + ".y", loc.getY());
        cfg.set(base + ".z", loc.getZ());
        cfg.set(base + ".yaw", loc.getYaw());
        cfg.set(base + ".pitch", loc.getPitch());

        // clear legacy keys if any
        cfg.set("LobbySpawn", null);
        cfg.set("GameSpawn", null);

        plugin.saveConfig();
        plugin.getSpawns().reload(plugin.getConfig());

        p.sendMessage("§a" + Character.toUpperCase(type.charAt(0)) + type.substring(1) + " spawn set!");
        return true;
    }

    public boolean teleport(Player p, String[] args) {
        if (!p.hasPermission("zombietag.admin")) { p.sendMessage("§cNo permission."); return true; }
        if (args.length != 2) { p.sendMessage("§eUsage: /zombietag teleport <lobby|game|exit>"); return true; }

        String which = args[1].toLowerCase();
        switch (which) {
            case "lobby": {
                var ls = spawns.lobby();
                if (ls == null) { p.sendMessage("§cLobby spawn not set or world missing."); return true; }
                p.teleport(ls);
                p.sendMessage("§aTeleported to lobby spawn.");
                return true;
            }
            case "game": {
                var gs = spawns.game();
                if (gs == null) { p.sendMessage("§cGame spawn not set or world missing."); return true; }
                p.teleport(gs);
                p.sendMessage("§aTeleported to game spawn.");
                return true;
            }
            case "exit": {
                var es = spawns.exit();
                if (es == null) { p.sendMessage("§cExit spawn not set or world missing."); return true; }
                p.teleport(es);
                p.sendMessage("§aTeleported to exit spawn.");
                return true;
            }
            default:
                p.sendMessage("§cInvalid teleport type! Use 'lobby', 'game', or 'exit'.");
                return true;
        }
    }

    public boolean info(Player p, String[] args) {
        if (!p.hasPermission("zombietag.admin")) { p.sendMessage("§cNo permission."); return true; }

        String section = (args.length >= 2) ? args[1].toLowerCase() : "all";

        java.util.function.Function<Boolean,String> yn = b -> b ? "§aON" : "§cOFF";
        java.util.function.Function<Integer,String> secs = v -> v + "s";
        java.util.function.Function<org.bukkit.Location,String> fmtLoc = loc -> {
            if (loc == null || loc.getWorld() == null) return "§7not set";
            return String.format("§f%s§7: §f%.1f§7, §f%.1f§7, §f%.1f §8(yaw %.1f, pitch %.1f)",
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        };

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
        org.bukkit.Location exitSpawn  = spawns.exit(); // NEW

        p.sendMessage("§8§m--------------------§r §aZombieTag §7Info §8§m--------------------");

        if (section.equals("all") || section.equals("lobby")) {
            p.sendMessage("§bLobby");
            p.sendMessage("  §7Needed: §f" + playerNeeded + "  §7Max: §f" + maxPlayers);
            p.sendMessage("  §7Countdown: §f" + secs.apply(lobbyCountdown));
            p.sendMessage("  §7Auto-rejoin: " + yn.apply(autoRejoin));
            p.sendMessage("  §7Queue during game: " + yn.apply(settings.queueDuringGame));
        }

        if (section.equals("all") || section.equals("game")) {
            p.sendMessage("§bGame");
            p.sendMessage("  §7Length: §f" + secs.apply(gameLen) + "  §7Grace: §f" + secs.apply(graceSeconds));
            p.sendMessage("  §7Announce length: " + yn.apply(announceLen));
        }

        if (section.equals("all") || section.equals("rewards")) {
            p.sendMessage("§bRewards (Survivors)");
            p.sendMessage("  §7Enabled: " + yn.apply(rewardEnabled));
            p.sendMessage("  §7Command: §f" + (rewardCmd == null ? "§7none" : rewardCmd));
        }

        if (section.equals("all") || section.equals("items")) {
            p.sendMessage("§bItems");
            p.sendMessage("  §7Zombie helmet: §f" + (helmetItem == null ? "§7default" : helmetItem));
        }

        if (section.equals("all") || section.equals("effects")) {
            p.sendMessage("§bEffects at Start");
            p.sendMessage("  §7Blindness: §f" + secs.apply(blindSec) + "  §7Night Vision: §f" + secs.apply(nightVisSec));
        }

        if (section.equals("all") || section.equals("stay") || section.equals("staystill")) {
            p.sendMessage("§bStay-Still");
            p.sendMessage("  §7Enabled: " + yn.apply(stayStillOn) + "  §7Time limit: §f" + secs.apply(stayStillSec));
            p.sendMessage("  §7Message: §f" + (stayStillMsg == null ? "§7default" : stayStillMsg));
        }

        if (section.equals("all") || section.equals("spawns") || section.equals("spawn")) {
            p.sendMessage("§bSpawns");
            p.sendMessage("  §7Lobby: " + fmtLoc.apply(lobbySpawn));
            p.sendMessage("  §7Game:  " + fmtLoc.apply(gameSpawn));
            p.sendMessage("  §7Exit:  " + fmtLoc.apply(exitSpawn)); // NEW
        }

        p.sendMessage("§8§m---------------------------------------------------------");
        return true;
    }
    public boolean end(Player p) {
        if (!p.hasPermission("zombietag.admin")) {
            p.sendMessage("§cNo permission.");
            return true;
        }

        // If there's no active game, just tell them
        if (!plugin.getGameState().isRunning()) {
            p.sendMessage("§eThere is no active Zombie Tag round.");
            return true;
        }

        // Force end the round immediately.
        // We pass 'false' for forceToLobby because your existing endGame(false)
        // already teleports players back to lobby/exit logic and resets state.
        game.endGame(false);

        p.sendMessage("§aZombie Tag round has been force-ended.");
        return true;
    }


    // ---- helpers (unchanged) ----
    private Location readSpawnFromConfig(String basePath) {
        FileConfiguration cfg = plugin.getConfig();

        String worldName = cfg.getString(basePath + ".world", null);
        if (worldName == null || worldName.isEmpty()) return null;

        World w = plugin.getServer().getWorld(worldName);
        if (w == null) return null;

        double x = cfg.getDouble(basePath + ".x");
        double y = cfg.getDouble(basePath + ".y");
        double z = cfg.getDouble(basePath + ".z");
        float yaw = (float) cfg.getDouble(basePath + ".yaw", 0.0);
        float pitch = (float) cfg.getDouble(basePath + ".pitch", 0.0);

        return new Location(w, x, y, z, yaw, pitch);
    }
}
