package com.jamplifier.zombietag.commands;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.*;
import com.jamplifier.zombietag.model.PlayerState;
import com.jamplifier.zombietag.stats.ConfigStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerCommands {
    private final MainClass plugin;
    private final LobbyService lobby;
    private final GameService game;
    private final ConfigStats stats;
    private final Settings settings;
    private final Spawns spawns;
    private final PlayerRegistry registry;

    public PlayerCommands(MainClass plugin, LobbyService lobby, GameService game, ConfigStats stats,
                          Settings settings, Spawns spawns, PlayerRegistry registry) {
        this.plugin = plugin; this.lobby = lobby; this.game = game; this.stats = stats;
        this.settings = settings; this.spawns = spawns; this.registry = registry;
    }

    public boolean help(Player p) {
        if (p.hasPermission("zombietag.admin")) {
            p.sendMessage("§eZombieTag Commands:");
            p.sendMessage("  §7/zombietag join");
            p.sendMessage("  §7/zombietag leave");
            p.sendMessage("  §7/zombietag top");
            p.sendMessage("  §7/zombietag stats [player]");
            p.sendMessage("  §7/zombietag setspawn <lobby|game> §8(admin)");
            p.sendMessage("  §7/zombietag teleport <lobby|game> §8(admin)");
            p.sendMessage("  §7/zombietag reload §8(admin)");
        } else {
            p.sendMessage("§eZombieTag Commands:");
            p.sendMessage("  §7/zombietag join");
            p.sendMessage("  §7/zombietag leave");
            p.sendMessage("  §7/zombietag top");
            p.sendMessage("  §7/zombietag stats [player]");
        }
        return true;
    }

    public boolean join(Player p) {
    	// already in lobby? stop the spam
        if (plugin.getGameState().getLobbyPlayers().contains(p)) {
            p.sendMessage("§eYou’re already in the lobby. (" +
                    plugin.getGameState().getLobbyPlayers().size() + "/" + settings.maxPlayers + ")");
            return true;
        }
        // If a game is running, queue in lobby and TP
        if (plugin.getGameState().isRunning()) {
            p.sendMessage("§eA game is in progress. You’ve been added to the lobby for the next round.");
            registry.getOrCreate(p.getUniqueId()); // ensure state exists
            teleportToLobby(p);
            if (!plugin.getGameState().getLobbyPlayers().contains(p)) {
                plugin.getGameState().getLobbyPlayers().add(p);
            }
            return true;
        }

        // Check lobby capacity
        int current = plugin.getGameState().getLobbyPlayers().size();
        if (current >= settings.maxPlayers) {
            p.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return true;
        }

        // Add to registry if not present
        PlayerState st = registry.getOrCreate(p.getUniqueId());
        st.setIngame(false);
        st.setZombie(false);

        // TP & enter lobby flow
        teleportToLobby(p);
        lobby.joinLobby(p);
        return true;
    }

    public boolean leave(Player p) {
        UUID id = p.getUniqueId();
        PlayerState st = registry.get(id);
        if (st == null || st.isIngame()) {
            p.sendMessage("§cYou are not currently in the lobby (or you’re in a game).");
            return true;
        }
        plugin.getGameState().getLobbyPlayers().remove(p);
        registry.remove(id);

        // If countdown threshold dips below needed, you may want to cancel countdown here.
        p.sendMessage("§aYou have left the lobby.");
        return true;
    }

    public boolean top(Player p) {
        p.sendMessage("§6--- Zombie Tag Leaderboard ---");

        // Top 5 Taggers
        List<Map.Entry<UUID, Integer>> taggers = stats.getLeaderboard("tags");
        p.sendMessage("§cTop 5 Taggers:");
        if (taggers.isEmpty()) {
            p.sendMessage("§7No data available.");
        } else {
            for (int i = 0; i < Math.min(5, taggers.size()); i++) {
                Map.Entry<UUID, Integer> e = taggers.get(i);
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                p.sendMessage("§e" + (i + 1) + ". §a" + (op.getName() == null ? e.getKey() : op.getName()) + ": §b" + e.getValue());
            }
        }

        // Top 5 Survivors
        List<Map.Entry<UUID, Integer>> surv = stats.getLeaderboard("survivals");
        p.sendMessage("§cTop 5 Survivors:");
        if (surv.isEmpty()) {
            p.sendMessage("§7No data available.");
        } else {
            for (int i = 0; i < Math.min(5, surv.size()); i++) {
                Map.Entry<UUID, Integer> e = surv.get(i);
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                p.sendMessage("§e" + (i + 1) + ". §a" + (op.getName() == null ? e.getKey() : op.getName()) + ": §b" + e.getValue());
            }
        }

        p.sendMessage("§6-----------------------------");
        return true;
    }

    public boolean stats(Player p, String[] args) {
        UUID target = p.getUniqueId();
        if (args.length == 2) {
            Player other = Bukkit.getPlayer(args[1]);
            if (other == null) { p.sendMessage("§cPlayer not found!"); return true; }
            target = other.getUniqueId();
            p.sendMessage("§6--- Zombie Tag Stats for " + other.getName() + " ---");
        } else {
            p.sendMessage("§6--- Your Zombie Tag Stats ---");
        }
        int tags = stats.getInt(target, "tags", 0);
        int surv = stats.getInt(target, "survivals", 0);
        p.sendMessage("§aTags: §e" + tags);
        p.sendMessage("§aSurvivals: §e" + surv);
        p.sendMessage("§6-------------------------------");
        return true;
    }

    // --- helpers ---
    private void teleportToLobby(Player p) {
        Location ls = spawns.lobby();
        if (ls != null) {
            p.teleport(ls);
            p.sendMessage("§aYou have been teleported to the lobby!");
        } else {
            p.sendMessage("§cLobby spawn is not set.");
        }
    }
}
