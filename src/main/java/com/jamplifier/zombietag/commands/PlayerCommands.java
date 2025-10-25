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

import static com.jamplifier.zombietag.Util.Lang.m;

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
            plugin.getLang().send(p, "commands.help.header_admin");
            plugin.getLang().send(p, "commands.help.join");
            plugin.getLang().send(p, "commands.help.leave");
            plugin.getLang().send(p, "commands.help.top");
            plugin.getLang().send(p, "commands.help.stats");
            plugin.getLang().send(p, "commands.help.setspawn");
            plugin.getLang().send(p, "commands.help.teleport");
            plugin.getLang().send(p, "commands.help.reload");
        } else {
            plugin.getLang().send(p, "commands.help.header_player");
            plugin.getLang().send(p, "commands.help.join");
            plugin.getLang().send(p, "commands.help.leave");
            plugin.getLang().send(p, "commands.help.top");
            plugin.getLang().send(p, "commands.help.stats");
        }
        return true;
    }

    public boolean join(Player p) {
        var phase = plugin.getGameState().getPhase();

        // Already queued?
        if (lobby.inLobby(p)) {
            plugin.getLang().send(p, "lobby.already_in",
                m("count", plugin.getGameState().getLobbyPlayers().size(), "max", settings.maxPlayers));
            return true;
        }

        // If a game is running/ending…
        if (phase == GamePhase.RUNNING || phase == GamePhase.ENDING) {
            if (!settings.queueDuringGame) {
                plugin.getLang().send(p, "lobby.blocked_while_running");
                return true;
            }
            // Queue for next round
            if (plugin.getGameState().getLobbyPlayers().size() >= settings.maxPlayers) {
                plugin.getLang().send(p, "lobby.full");
                return true;
            }
            var st = registry.getOrCreate(p.getUniqueId());
            st.setIngame(false);
            st.setZombie(false);

            teleportToLobby(p);
            // Let LobbyService handle adds/messages (it won’t start countdown while phase != LOBBY)
            lobby.joinLobby(p);
            plugin.getLang().send(p, "lobby.queued");
            return true;
        }

        // Normal (no game running): capacity → TP → join
        if (plugin.getGameState().getLobbyPlayers().size() >= settings.maxPlayers) {
            plugin.getLang().send(p, "lobby.full");
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
            plugin.getLang().send(p, "lobby.cant_leave_in_game");
            return true;
        }

        // Must actually be queued in the lobby
        if (!lobby.inLobby(p)) {
            plugin.getLang().send(p, "lobby.not_in");
            return true;
        }

        // Remove from lobby first
        lobby.leaveLobby(p);

        // Teleport to exit (fallbacks: lobby world spawn → current world spawn)
        teleportToExit(p);
        return true;
    }

    public boolean top(Player p) {
        plugin.getLang().send(p, "commands.top.header");

        // Top 5 Taggers
        List<Map.Entry<UUID, Integer>> taggers = stats.getLeaderboard("tags");
        plugin.getLang().send(p, "commands.top.taggers_title");
        if (taggers.isEmpty()) {
            plugin.getLang().send(p, "commands.top.no_data");
        } else {
            for (int i = 0; i < Math.min(5, taggers.size()); i++) {
                Map.Entry<UUID, Integer> e = taggers.get(i);
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                String name = (op.getName() == null ? e.getKey().toString() : op.getName());
                plugin.getLang().send(p, "commands.top.line", m("rank", i + 1, "name", name, "value", e.getValue()));
            }
        }

        // Top 5 Survivors
        List<Map.Entry<UUID, Integer>> surv = stats.getLeaderboard("survivals");
        plugin.getLang().send(p, "commands.top.survivors_title");
        if (surv.isEmpty()) {
            plugin.getLang().send(p, "commands.top.no_data");
        } else {
            for (int i = 0; i < Math.min(5, surv.size()); i++) {
                Map.Entry<UUID, Integer> e = surv.get(i);
                OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
                String name = (op.getName() == null ? e.getKey().toString() : op.getName());
                plugin.getLang().send(p, "commands.top.line", m("rank", i + 1, "name", name, "value", e.getValue()));
            }
        }

        plugin.getLang().send(p, "commands.top.footer");
        return true;
    }

    public boolean stats(Player p, String[] args) {
        UUID target = p.getUniqueId();
        if (args.length == 2) {
            Player other = Bukkit.getPlayer(args[1]);
            if (other == null) { plugin.getLang().send(p, "commands.player_not_found"); return true; }
            target = other.getUniqueId();
            plugin.getLang().send(p, "commands.stats.header_other", m("name", other.getName()));
        } else {
            plugin.getLang().send(p, "commands.stats.header_self");
        }
        int tagsVal = stats.getInt(target, "tags", 0);
        int survVal = stats.getInt(target, "survivals", 0);
        plugin.getLang().send(p, "commands.stats.tags", m("value", tagsVal));
        plugin.getLang().send(p, "commands.stats.survivals", m("value", survVal));
        plugin.getLang().send(p, "commands.stats.footer");
        return true;
    }

    // --- helpers ---
    private void teleportToLobby(Player p) {
        Location ls = spawns.lobby();
        if (ls != null) {
            p.teleport(ls);
            plugin.getLang().send(p, "lobby.teleported");
        } else {
            plugin.getLang().send(p, "lobby.missing_lobby_spawn");
        }
    }
    private void teleportToExit(Player p) {
        Location es = spawns.exit();
        if (es != null) {
            p.teleport(es);
            // no message here anymore
            return;
        }

        // fallback 1: lobby world spawn if lobby set
        Location ls = spawns.lobby();
        if (ls != null && ls.getWorld() != null) {
            p.teleport(ls.getWorld().getSpawnLocation());
            p.sendMessage("§7No Exit spawn set; sent to lobby world spawn.");
            return;
        }

        // fallback 2: current world spawn
        p.teleport(p.getWorld().getSpawnLocation());
        p.sendMessage("§7No Exit spawn set; sent to world spawn.");
    }
}
