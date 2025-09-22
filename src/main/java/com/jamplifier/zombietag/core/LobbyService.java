// core/LobbyService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import static com.jamplifier.zombietag.Util.Lang.m;

public class LobbyService {
    private final MainClass plugin;
    private final Settings settings;
    private final Spawns spawns;
    private final GameState state;

    public LobbyService(MainClass plugin, Settings settings, Spawns spawns, GameState state) {
        this.plugin = plugin; this.settings = settings; this.spawns = spawns; this.state = state;
    }

    public boolean inLobby(Player p) { return state.getLobbyPlayers().contains(p); }

    public void joinLobby(Player p) {
        // Allow queuing during RUNNING/ENDING only if enabled
        if ((state.getPhase() == GamePhase.RUNNING || state.getPhase() == GamePhase.ENDING)
                && !settings.queueDuringGame) {
            plugin.getLang().send(p, "lobby.blocked_while_running");
            return;
        }

        // Already part of an active round?
        if (state.getGamePlayers().contains(p)) {
            plugin.getLang().send(p, "lobby.already_in_round");
            return;
        }

        var lobby = state.getLobbyPlayers();

        // full?
        if (lobby.size() >= settings.maxPlayers) {
            plugin.getLang().send(p, "lobby.full");
            return;
        }

        // already in lobby?
        if (lobby.contains(p)) {
            plugin.getLang().send(p, "lobby.already_in", m("count", lobby.size(), "max", settings.maxPlayers));
            return;
        }

        // add + announce
        lobby.add(p);
        plugin.getLang().send(lobby, "lobby.joined", m("player", p.getName(), "count", lobby.size(), "max", settings.maxPlayers));

        // decide what to do next
        int needed = Math.max(0, settings.playerNeeded - lobby.size());

        if (needed <= 0) {
            // threshold met
            if (state.getPhase() == GamePhase.LOBBY && state.getLobbyCountdownTask() == null) {
                startLobbyCountdown();  // sets phase=COUNTDOWN and starts the task
            } else if (state.isCountdown()) {
                plugin.getLang().send(p, "lobby.countdown_running");
            }
            return;
        }

        // still waiting (don’t spam if countdown already started)
        if (!state.isCountdown()) {
            plugin.getLang().send(lobby, "lobby.waiting", m("needed", needed, "plural", (needed == 1 ? "" : "s")));
        }
    }

    public void leaveLobby(Player p) {
        if (state.getLobbyPlayers().remove(p)) {
            plugin.getLang().send(p, "lobby.left");
            int cur = state.getLobbyPlayers().size();
            plugin.getLang().send(state.getLobbyPlayers(), "lobby.count", m("count", cur, "max", settings.maxPlayers));
        } else {
            plugin.getLang().send(p, "lobby.not_in");
        }
    }

    private void startLobbyCountdown() {
        // defensive: kill stale reference if any
        if (state.getLobbyCountdownTask() != null) {
            try { state.getLobbyCountdownTask().cancel(); } catch (Throwable ignored) {}
            state.setLobbyCountdownTask(null);
        }

        state.setPhase(GamePhase.COUNTDOWN);
        BukkitRunnable task = new BukkitRunnable() {
            int seconds = settings.lobbyCountdownSeconds;

            @Override public void run() {
                var lobby = state.getLobbyPlayers();
                lobby.removeIf(p -> p == null || !p.isOnline()); // keep fresh

                if (lobby.size() < settings.playerNeeded) {
                    plugin.getLang().send(lobby, "lobby.countdown_canceled");
                    state.setPhase(GamePhase.LOBBY);
                    this.cancel();
                    state.setLobbyCountdownTask(null);
                    return;
                }

                if (seconds == 0) {
                    this.cancel();
                    state.setLobbyCountdownTask(null);
                    plugin.getGameService().startGameFromLobby();
                    return;
                }

                if (seconds <= 5 || seconds % 5 == 0) {
                    plugin.getLang().send(lobby, "lobby.countdown_tick", m("seconds", seconds));
                }
                seconds--;
            }
        };

        state.setLobbyCountdownTask(task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void maybeStartCountdown() {
        // Block if game spawn isn't configured
        if (spawns.game() == null) {
            plugin.getLang().send(state.getLobbyPlayers(), "lobby.missing_game_spawn");
            return;
        }

        var lobby = state.getLobbyPlayers();
        // only count online players
        lobby.removeIf(p -> p == null || !p.isOnline());

        // Clear stale countdown ref; if a valid one exists, do nothing
        if (state.getLobbyCountdownTask() != null) {
            if (state.getLobbyCountdownTask().isCancelled()) {
                state.setLobbyCountdownTask(null);
            } else {
                return; // countdown already running — don't restart it
            }
        }

        // Guardrails: only start in LOBBY, and not if any game players are active
        if (state.getPhase() != GamePhase.LOBBY) return;
        if (!state.getGamePlayers().isEmpty()) return;

        int size = lobby.size();
        int need = settings.playerNeeded;
        if (size >= need) {
            startLobbyCountdown();
        } else {
            plugin.getLang().send(lobby, "lobby.debug_waiting", m("count", size, "need", need));
        }
    }
}
