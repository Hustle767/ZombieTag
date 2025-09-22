package com.jamplifier.zombietag.commands;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.*;
import com.jamplifier.zombietag.model.PlayerState;
import com.jamplifier.zombietag.stats.ConfigStats;
import com.jamplifier.zombietag.core.GamePhase;
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
        var phase = plugin.getGameState().getPhase();

        // Already queued?
        if (lobby.inLobby(p)) {
            p.sendMessage("§eYou’re already in the lobby. (" +
                plugin.getGameState().getLobbyPlayers().size() + "/" + settings.maxPlayers + ")");
            return true;
        }

        // If a game is running/ending…
        if (phase == GamePhase.RUNNING || phase == GamePhase.ENDING) {
            if (!settings.queueDuringGame) {
                p.sendMessage("§cA game is currently running. Please wait for the next round.");
                return true;
            }
            // Queue for next round
            if (plugin.getGameState().getLobbyPlayers().size() >= settings.maxPlayers) {
                p.sendMessage("§cThe lobby is full! Please wait for the next round.");
                return true;
            }
            var st = registry.getOrCreate(p.getUniqueId());
            st.setIngame(false);
            st.setZombie(false);

            teleportToLobby(p);
            // Let LobbyService handle adds/messages (it won’t start countdown while phase != LOBBY)
            lobby.joinLobby(p);
            p.sendMessage("§aYou’ve been added to the queue for the next round.");
            return true;
        }

        // Normal (no game running): capacity → TP → join
        if (plugin.getGameState().getLobbyPlayers().size() >= settings.maxPlayers) {
            p.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return true;
        }
        var st = registry.getOrCreate(p.getUniqueId());
        st.setIngame(false);
        st.setZombie(false);
        teleportToLobby(p);
        lobby.joinLobby(p);
        return true;
    }



    public boolean leave(Player p) {
        // If they’re in a running game, this command shouldn’t apply
        if (plugin.getGameState().isRunning() && plugin.getGameState().getGamePlayers().contains(p)) {
            p.sendMessage("§cYou’re in an active round; you can’t leave the lobby.");
            return true;
        }

        // Must actually be queued in the lobby
        if (!lobby.inLobby(p)) {
            p.sendMessage("§cYou are not in the lobby.");
            return true;
        }

        // Let LobbyService handle the removal + broadcast/message
        lobby.leaveLobby(p);
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
